package com.atlassian.awsgenie.terminator;

import com.amazonaws.arn.Arn;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.DeleteAlarmsRequest;
import com.amazonaws.services.sns.AmazonSNS;
import com.atlassian.awsgenie.fetcher.ResourceFetcher;
import com.atlassian.awsgenie.fetcher.ResourceFetcherConfiguration;
import com.atlassian.awsgenie.fetcher.ResourceFetcherFactory;
import com.atlassian.awsgenie.fetcher.Service;
import com.atlassian.awsgenie.fetcher.credentials.AWSClientConfiguration;
import com.atlassian.awsgenie.fetcher.credentials.AWSClientProvider;
import com.atlassian.awsgenie.fetcher.sns.SNSResource;
import com.atlassian.awsgenie.terminator.configuration.Configuration;
import com.atlassian.awsgenie.terminator.interceptor.InterceptorRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Celal Emre CICEK
 * @version 6.04.2021
 */

public class SNSResourceTerminator extends ResourceTerminatorWithProvider implements ResourceTerminator<SNSResource> {
    private static final Logger LOGGER = LogManager.getLogger(SNSResourceTerminator.class);

    private ResourceFetcherFactory<SNSResource> resourceFetcherFactory;

    public SNSResourceTerminator(AWSClientConfiguration configuration) {
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
        AmazonSNS snsClient = AWSClientProvider.getInstance(getConfiguration()).getAmazonSNS();

        AmazonCloudWatch cloudWatchClient = AWSClientProvider.getInstance(getConfiguration()).getAmazonCloudWatch();

        HashSet<String> topicsToDelete = new LinkedHashSet<>();
        HashSet<String> cloudwatchAlarmsToDelete = new LinkedHashSet<>();

        List<String> details = new LinkedList<>();

        ResourceFetcher<SNSResource> fetcher = getFetchResourceFactory().getFetcher(service, new ResourceFetcherConfiguration(getConfiguration()));
        List<SNSResource> snsResourceList = fetcher.fetchResources(region, resources, details);

        for (SNSResource snsResource : snsResourceList) {
            String topicName = snsResource.getResourceName();
            Double publishCountInLastWeek = (Double) fetcher.getUsage(region, topicName, lastUsage);
            if (publishCountInLastWeek > 0) {
                if (force) {
                    details.add("Topic seems in use, but still deleting with force: [" + topicName + "], totalUsage: [" + publishCountInLastWeek + "]");
                    LOGGER.warn("Topic seems in use, but still deleting with force: [" + topicName + "], totalUsage: [" + publishCountInLastWeek + "]");
                } else {
                    details.add("Topic seems in use, not deleting: [" + topicName + "], totalUsage: [" + publishCountInLastWeek + "]");
                    LOGGER.warn("Topic seems in use, not deleting: [" + topicName + "], totalUsage: [" + publishCountInLastWeek + "]");
                    continue;
                }
            }
            topicsToDelete.add(topicName);
            cloudwatchAlarmsToDelete.addAll(snsResource.getCloudwatchAlarms());
        }

        StringBuilder info = new StringBuilder()
                .append("The resources will be terminated regarding ").append(ticket).append("\n")
                .append("* Dry-Run: ").append(!apply).append("\n")
                .append("* Topics: ").append(topicsToDelete).append("\n")
                .append("* Cloudwatch alarms: ").append(cloudwatchAlarmsToDelete).append("\n");

        info.append("Details:\n");
        details.forEach(d -> info.append("-- ").append(d).append("\n"));
        LOGGER.info(info);

        InterceptorRegistry.getBeforeTerminateInterceptors()
                .forEach(interceptor -> interceptor.intercept(service, snsResourceList, info.toString(), apply));

        if (apply) {
            LOGGER.info("Terminating the resources...");

            topicsToDelete.forEach(snsClient::deleteTopic);

            if (!cloudwatchAlarmsToDelete.isEmpty()) {
                cloudWatchClient.deleteAlarms(new DeleteAlarmsRequest().withAlarmNames(cloudwatchAlarmsToDelete));
            }
        }

        InterceptorRegistry.getAfterTerminateInterceptors()
                .forEach(interceptor -> interceptor.intercept(service, snsResourceList, info.toString(), apply));

        LOGGER.info("Succeed.");
    }

    private String getResourceFromArn(String arn) {
        return Arn.fromString(arn).getResource().getResource();
    }


    void setFetchResourceFactory(ResourceFetcherFactory<SNSResource> resourceFetcherFactory) {
        this.resourceFetcherFactory = resourceFetcherFactory;
    }

    private ResourceFetcherFactory<SNSResource> getFetchResourceFactory() {
        if (this.resourceFetcherFactory != null) {
            return this.resourceFetcherFactory;
        } else {
            return new ResourceFetcherFactory<>();
        }
    }
}