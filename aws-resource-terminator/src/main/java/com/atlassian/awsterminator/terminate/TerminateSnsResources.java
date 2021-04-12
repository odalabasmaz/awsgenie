package com.atlassian.awsterminator.terminate;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.DeleteAlarmsRequest;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.atlassian.awsterminator.interceptor.InterceptorRegistry;
import com.atlassian.awstool.terminate.FetchResourceFactory;
import com.atlassian.awstool.terminate.FetchResources;
import com.atlassian.awstool.terminate.Service;
import com.atlassian.awstool.terminate.sns.SNSResource;
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

public class TerminateSnsResources implements TerminateResources<SNSResource> {
    private static final Logger LOGGER = LogManager.getLogger(TerminateSnsResources.class);

    private final AWSCredentialsProvider credentialsProvider;
    private AmazonCloudWatch cloudWatchClient;
    private AmazonSNS snsClient;
    private FetchResourceFactory<SNSResource> fetchResourceFactory;

    public TerminateSnsResources(AWSCredentialsProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
    }

    @Override
    public void terminateResource(String region, Service service, List<String> resources, String ticket, boolean apply) throws Exception {
        AmazonSNS snsClient = getSnsClient(region);
        AmazonCloudWatch cloudWatchClient = getCloudWatchClient(region);

        HashSet<String> topicsToDelete = new LinkedHashSet<>();
        HashSet<String> cloudwatchAlarmsToDelete = new LinkedHashSet<>();
        List<String> details = new LinkedList<>();

        FetchResources<SNSResource> fetcher = getFetchResourceFactory().getFetcher(Service.SNS, credentialsProvider);
        List<SNSResource> snsResourceList = fetcher.fetchResources(region, resources, details);

        for (SNSResource snsResource : snsResourceList) {
            if (snsResource.getPublishCountInLastWeek() > 0) {
                details.add("Topic seems in use, not deleting: [" + snsResource.getResourceName() + "], totalUsage: [" + snsResource.getPublishCountInLastWeek() + "]");
                LOGGER.warn("Topic seems in use, not deleting: [" + snsResource.getResourceName() + "], totalUsage: [" + snsResource.getPublishCountInLastWeek() + "]");
                continue;
            }
            topicsToDelete.add(snsResource.getResourceName());
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

    void setCloudWatchClient(AmazonCloudWatch cloudWatchClient) {
        this.cloudWatchClient = cloudWatchClient;
    }

    private AmazonCloudWatch getCloudWatchClient(String region) {
        if (this.cloudWatchClient != null) {
            return this.cloudWatchClient;
        } else {
            return AmazonCloudWatchClient
                    .builder()
                    .withRegion(region)
                    .withCredentials(credentialsProvider)
                    .build();
        }
    }

    void setSnsClient(AmazonSNS snsClient) {
        this.snsClient = snsClient;
    }

    private AmazonSNS getSnsClient(String region) {
        if (this.snsClient != null) {
            return this.snsClient;
        } else {
            return AmazonSNSClient
                    .builder()
                    .withRegion(region)
                    .withCredentials(credentialsProvider)
                    .build();
        }
    }

    void setFetchResourceFactory(FetchResourceFactory<SNSResource> fetchResourceFactory) {
        this.fetchResourceFactory = fetchResourceFactory;
    }

    private FetchResourceFactory<SNSResource> getFetchResourceFactory() {
        if (this.fetchResourceFactory != null) {
            return this.fetchResourceFactory;
        } else {
            return new FetchResourceFactory<>();
        }
    }
}
