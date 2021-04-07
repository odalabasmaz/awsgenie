package com.atlassian.awsterminator.terminate;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.DeleteAlarmsRequest;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.atlassian.awstool.terminate.FetchResourceFactory;
import com.atlassian.awstool.terminate.FetchResources;
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

public class TerminateDynamoDBResources implements TerminateResources {
    private static final Logger LOGGER = LogManager.getLogger(TerminateDynamoDBResources.class);

    private final AWSCredentialsProvider credentialsProvider;

    public TerminateDynamoDBResources(AWSCredentialsProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
    }

    @Override
    public void terminateResource(String region, String service, List<String> resources, String ticket, boolean apply) throws Exception {
        AmazonDynamoDB dynamoDBClient = AmazonDynamoDBClient
                .builder()
                .withRegion(region)
                .withCredentials(credentialsProvider)
                .build();

        AmazonCloudWatch cloudWatchClient = AmazonCloudWatchClient
                .builder()
                .withRegion(region)
                .withCredentials(credentialsProvider)
                .build();

        // Resources to be removed
        LinkedHashSet<String> tablesToDelete = new LinkedHashSet<>();
        LinkedHashSet<String> cloudwatchAlarmsToDelete = new LinkedHashSet<>();

        List<String> details = new LinkedList<>();

        FetchResources fetcher = new FetchResourceFactory().getFetcher("dynamodb", credentialsProvider);
        List<DynamodbResource> cloudwatchResourceList = (List<DynamodbResource>) fetcher.fetchResources(region, service, resources, details);

        for (DynamodbResource dynamodbResource : cloudwatchResourceList) {
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

        if (apply) {
            LOGGER.info("Terminating the resources...");

            tablesToDelete.forEach(dynamoDBClient::deleteTable);

            if (!cloudwatchAlarmsToDelete.isEmpty()) {
                cloudWatchClient.deleteAlarms(new DeleteAlarmsRequest().withAlarmNames(cloudwatchAlarmsToDelete));
            }
        }

        LOGGER.info("Succeed.");
    }
}
