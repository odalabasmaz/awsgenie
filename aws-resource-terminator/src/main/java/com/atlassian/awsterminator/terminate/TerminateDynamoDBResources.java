package com.atlassian.awsterminator.terminate;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.DeleteAlarmsRequest;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.atlassian.awsterminator.interceptor.InterceptorRegistry;
import com.atlassian.awstool.terminate.FetchResourceFactory;
import com.atlassian.awstool.terminate.FetchResources;
import com.atlassian.awstool.terminate.Service;
import com.atlassian.awstool.terminate.dynamodb.DynamodbResource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Orhun Dalabasmaz
 * @version 10.03.2021
 */

public class TerminateDynamoDBResources implements TerminateResources<DynamodbResource> {
    private static final Logger LOGGER = LogManager.getLogger(TerminateDynamoDBResources.class);

    private final AWSCredentialsProvider credentialsProvider;
    private AmazonCloudWatch cloudWatchClient;
    private AmazonDynamoDB dynamoDBClient;
    private FetchResourceFactory<DynamodbResource> fetchResourceFactory;

    public TerminateDynamoDBResources(AWSCredentialsProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
    }

    @Override
    public void terminateResource(String region, Service service, List<String> resources, String ticket, boolean apply) throws Exception {
        AmazonDynamoDB dynamoDBClient = getDynamoDBClient(region);
        AmazonCloudWatch cloudWatchClient = getCloudWatchClient(region);

        // Resources to be removed
        LinkedHashSet<String> tablesToDelete = new LinkedHashSet<>();
        LinkedHashSet<String> cloudwatchAlarmsToDelete = new LinkedHashSet<>();

        List<String> details = new LinkedList<>();

        FetchResources<DynamodbResource> fetcher = getFetchResourceFactory().getFetcher(Service.DYNAMODB, credentialsProvider);
        List<DynamodbResource> dynamodbResourceList = fetcher.fetchResources(region, resources, details);

        for (DynamodbResource dynamodbResource : dynamodbResourceList) {
            if (dynamodbResource.getTotalUsage() > 0) {
                details.add("DynamoDB table seems in use, not deleting: [" + dynamodbResource.getResourceName() + "], totalUsage: [" + dynamodbResource.getTotalUsage() + "]");
                LOGGER.warn("DynamoDB table seems in use, not deleting: [" + dynamodbResource.getResourceName() + "], totalUsage: [" + dynamodbResource.getTotalUsage() + "]");
                continue;
            }
            tablesToDelete.add(dynamodbResource.getResourceName());
            cloudwatchAlarmsToDelete.addAll(dynamodbResource.getCloudwatchAlarmList());
        }

        StringBuilder info = new StringBuilder()
                .append("The resources will be terminated regarding ").append(ticket).append("\n")
                .append("* Dry-Run: ").append(!apply).append("\n")
                .append("* DynamoDB tables: ").append(tablesToDelete).append("\n")
                .append("* Cloudwatch alarms: ").append(cloudwatchAlarmsToDelete).append("\n");

        info.append("Details:\n");
        details.forEach(d -> info.append("-- ").append(d).append("\n"));
        LOGGER.info(info);

        InterceptorRegistry.getBeforeTerminateInterceptors()
                .forEach(interceptor -> interceptor.intercept(service, dynamodbResourceList, info.toString(), apply));

        if (apply) {
            LOGGER.info("Terminating the resources...");

            tablesToDelete.forEach(dynamoDBClient::deleteTable);

            if (!cloudwatchAlarmsToDelete.isEmpty()) {
                cloudWatchClient.deleteAlarms(new DeleteAlarmsRequest().withAlarmNames(cloudwatchAlarmsToDelete));
            }
        }

        InterceptorRegistry.getAfterTerminateInterceptors()
                .forEach(interceptor -> interceptor.intercept(service, dynamodbResourceList, info.toString(), apply));

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

    void setDynamoDBClient(AmazonDynamoDB dynamoDBClient) {
        this.dynamoDBClient = dynamoDBClient;
    }

    private AmazonDynamoDB getDynamoDBClient(String region) {
        if (this.dynamoDBClient != null) {
            return this.dynamoDBClient;
        } else {
            return AmazonDynamoDBClient
                    .builder()
                    .withRegion(region)
                    .withCredentials(credentialsProvider)
                    .build();
        }
    }

    void setFetchResourceFactory(FetchResourceFactory<DynamodbResource> fetchResourceFactory) {
        this.fetchResourceFactory = fetchResourceFactory;
    }

    private FetchResourceFactory<DynamodbResource> getFetchResourceFactory() {
        if (this.fetchResourceFactory != null) {
            return this.fetchResourceFactory;
        } else {
            return new FetchResourceFactory<>();
        }
    }
}
