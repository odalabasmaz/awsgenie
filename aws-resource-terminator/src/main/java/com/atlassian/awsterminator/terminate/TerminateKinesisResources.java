package com.atlassian.awsterminator.terminate;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.DeleteAlarmsRequest;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClient;
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
    public void terminateResource(String region, Service service, List<String> resources, String ticket, boolean apply) throws Exception {
        AmazonKinesis kinesisClient = AwsClientProvider.getInstance(getConfiguration()).getAmazonKinesis();

        AmazonCloudWatch cloudWatchClient = AwsClientProvider.getInstance(getConfiguration()).getAmazonCloudWatch();

        // Resources to be removed
        LinkedHashSet<String> streamsToDelete = new LinkedHashSet<>();
        LinkedHashSet<String> cloudwatchAlarmsToDelete = new LinkedHashSet<>();

        List<String> details = new LinkedList<>();

        FetchResources fetcher = getFetchResourceFactory().getFetcher(Service.KINESIS, new FetcherConfiguration(getConfiguration()));
        List<KinesisResource> kinesisResourceList = (List<KinesisResource>) fetcher.fetchResources(region, resources, details);

        for (KinesisResource kinesisResource : kinesisResourceList) {
            Double totalUsage = (Double) fetcher.getUsage(region, kinesisResource.getResourceName());
            if (totalUsage > 0) {
                details.add("Kinesis stream seems in use, not deleting: [" + kinesisResource.getResourceName() +
                        "], totalUsage: [" + totalUsage + "]");
                LOGGER.warn("Kinesis stream seems in use, not deleting: [" + kinesisResource.getResourceName() +
                        "], totalUsage: [" + totalUsage + "]");
                continue;
            }

            streamsToDelete.add(kinesisResource.getResourceName());
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
