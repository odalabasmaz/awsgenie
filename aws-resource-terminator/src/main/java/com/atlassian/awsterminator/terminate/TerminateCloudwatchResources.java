package com.atlassian.awsterminator.terminate;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.DeleteAlarmsRequest;
import com.atlassian.awsterminator.interceptor.InterceptorRegistry;
import com.atlassian.awstool.terminate.FetchResourceFactory;
import com.atlassian.awstool.terminate.FetchResources;
import com.atlassian.awstool.terminate.cloudwatch.CloudwatchResource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Orhun Dalabasmaz
 * @version 10.03.2021
 */

public class TerminateCloudwatchResources implements TerminateResources {
    private static final Logger LOGGER = LogManager.getLogger(TerminateCloudwatchResources.class);

    private final AWSCredentialsProvider credentialsProvider;
    private AmazonCloudWatch cloudWatchClient;
    private FetchResourceFactory fetchResourceFactory;

    public TerminateCloudwatchResources(AWSCredentialsProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
    }

    @Override
    public void terminateResource(String region, String service, List<String> resources, String ticket, boolean apply) throws Exception {
        AmazonCloudWatch cloudWatchClient = getCloudWatchClient(region);

        FetchResources fetcher = getFetchResourceFactory().getFetcher("cloudwatch", credentialsProvider);
        List<CloudwatchResource> cloudwatchResourceList = (List<CloudwatchResource>) fetcher.fetchResources(region, resources, null);

        Set<String> cloudwatchAlarmsToDelete = new HashSet<>();
        Set<String> cloudwatchAlarmsNotToDelete = new HashSet<>(resources);
        for (CloudwatchResource cloudwatchResource : cloudwatchResourceList) {
            cloudwatchAlarmsNotToDelete.remove(cloudwatchResource.getResourceName());
            cloudwatchAlarmsToDelete.add(cloudwatchResource.getResourceName());
        }

        StringBuilder info = new StringBuilder()
                .append("The resources will be terminated regarding ").append(ticket).append("\n")
                .append("* Dry-Run: ").append(!apply).append("\n")
                .append("* Cloudwatch alarms to be deleted: ").append(cloudwatchAlarmsToDelete).append("\n")
                .append("* Cloudwatch alarms not found: ").append(cloudwatchAlarmsNotToDelete).append("\n");
        LOGGER.info(info);

        InterceptorRegistry.getBeforeTerminateInterceptors()
                .forEach(interceptor -> interceptor.intercept(service, cloudwatchResourceList, info.toString(), apply));

        if (apply) {
            LOGGER.info("Terminating the resources...");
            if (!cloudwatchAlarmsToDelete.isEmpty()) {
                cloudWatchClient.deleteAlarms(new DeleteAlarmsRequest().withAlarmNames(cloudwatchAlarmsToDelete));
            }
        }

        InterceptorRegistry.getAfterTerminateInterceptors()
                .forEach(interceptor -> interceptor.intercept(service, cloudwatchResourceList, info.toString(), apply));

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

    void setFetchResourceFactory(FetchResourceFactory fetchResourceFactory) {
        this.fetchResourceFactory = fetchResourceFactory;
    }

    private FetchResourceFactory getFetchResourceFactory() {
        if (this.fetchResourceFactory != null) {
            return this.fetchResourceFactory;
        } else {
            return new FetchResourceFactory();
        }
    }
}
