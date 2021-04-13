package com.atlassian.awsterminator.terminate;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.DeleteAlarmsRequest;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.atlassian.awsterminator.configuration.Configuration;
import com.atlassian.awsterminator.interceptor.InterceptorRegistry;
import com.atlassian.awstool.terminate.FetchResourceFactory;
import com.atlassian.awstool.terminate.FetchResources;
import com.atlassian.awstool.terminate.FetcherConfiguration;
import com.atlassian.awstool.terminate.Service;
import com.atlassian.awstool.terminate.kinesis.KinesisResource;
import credentials.AwsClientConfiguration;
import credentials.AwsClientProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Celal Emre CICEK
 * @version 7.04.2021
 */

public class TerminateKinesisResources extends TerminateResourcesWithProvider implements TerminateResources<KinesisResource> {
    private static final Logger LOGGER = LogManager.getLogger(TerminateKinesisResources.class);

    private FetchResourceFactory<KinesisResource> fetchResourceFactory;

    public TerminateKinesisResources(AwsClientConfiguration configuration) {
        super(configuration);
    }

    @Override
    public void terminateResource(Configuration conf, boolean apply) throws Exception {
        terminateResource(conf.getRegion(), Service.fromValue(conf.getService()), conf.getResourcesAsList(), conf.getDescription(),
                conf.getLastUsage(), conf.isForce(), apply);
    }

    @Override
    public void terminateResource(String region, Service service, List<String> resources, String ticket, boolean apply) throws Exception {
        terminateResource(region, service, resources, ticket, 7, false, apply);
    }

    @Override
    public void terminateResource(String region, Service service, List<String> resources, String ticket, int lastUsage, boolean force, boolean apply) throws Exception {
        AmazonKinesis kinesisClient = AwsClientProvider.getInstance(getConfiguration()).getAmazonKinesis();

        AmazonCloudWatch cloudWatchClient = AwsClientProvider.getInstance(getConfiguration()).getAmazonCloudWatch();

        // Resources to be removed
        LinkedHashSet<String> streamsToDelete = new LinkedHashSet<>();
        LinkedHashSet<String> cloudwatchAlarmsToDelete = new LinkedHashSet<>();

        List<String> details = new LinkedList<>();

        FetchResources<KinesisResource> fetcher = getFetchResourceFactory().getFetcher(service, new FetcherConfiguration(getConfiguration()));
        List<KinesisResource> kinesisResourceList = fetcher.fetchResources(region, resources, details);

        for (KinesisResource kinesisResource : kinesisResourceList) {
            String streamName = kinesisResource.getResourceName();
            Double totalUsage = (Double) fetcher.getUsage(region, streamName, lastUsage);
            if (totalUsage > 0) {
                if (force) {
                    details.add("Kinesis stream seems in use, but still deleting with force: [" + streamName + "], totalUsage: [" + totalUsage + "]");
                    LOGGER.warn("Kinesis stream seems in use, but still deleting with force: [" + streamName + "], totalUsage: [" + totalUsage + "]");
                } else {
                    details.add("Kinesis stream seems in use, not deleting: [" + streamName + "], totalUsage: [" + totalUsage + "]");
                    LOGGER.warn("Kinesis stream seems in use, not deleting: [" + streamName + "], totalUsage: [" + totalUsage + "]");
                    continue;
                }
            }

            streamsToDelete.add(streamName);
            cloudwatchAlarmsToDelete.addAll(kinesisResource.getCloudwatchAlarmList());
        }

        StringBuilder info = new StringBuilder()
                .append("The resources will be terminated regarding ").append(ticket).append("\n")
                .append("* Dry-Run: ").append(!apply).append("\n")
                .append("* Kinesis streams: ").append(streamsToDelete).append("\n")
                .append("* Cloudwatch alarms: ").append(cloudwatchAlarmsToDelete).append("\n");

        info.append("Details:\n");
        details.forEach(d -> info.append("-- ").append(d).append("\n"));
        LOGGER.info(info);

        InterceptorRegistry.getBeforeTerminateInterceptors()
                .forEach(interceptor -> interceptor.intercept(service, kinesisResourceList, info.toString(), apply));

        if (apply) {
            LOGGER.info("Terminating the resources...");

            streamsToDelete.forEach(kinesisClient::deleteStream);

            if (!cloudwatchAlarmsToDelete.isEmpty()) {
                cloudWatchClient.deleteAlarms(new DeleteAlarmsRequest().withAlarmNames(cloudwatchAlarmsToDelete));
            }
        }

        InterceptorRegistry.getAfterTerminateInterceptors()
                .forEach(interceptor -> interceptor.intercept(service, kinesisResourceList, info.toString(), apply));

        LOGGER.info("Succeed.");
    }

    void setFetchResourceFactory(FetchResourceFactory<KinesisResource> fetchResourceFactory) {
        this.fetchResourceFactory = fetchResourceFactory;
    }

    private FetchResourceFactory<KinesisResource> getFetchResourceFactory() {
        if (this.fetchResourceFactory != null) {
            return this.fetchResourceFactory;
        } else {
            return new FetchResourceFactory<>();
        }
    }
}
