package com.atlassian.awsterminator.terminate;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.DeleteAlarmsRequest;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClient;
import com.amazonaws.services.lambda.model.DeleteEventSourceMappingRequest;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.atlassian.awstool.terminate.FetchResourceFactory;
import com.atlassian.awstool.terminate.FetchResources;
import com.atlassian.awstool.terminate.sqs.SQSResource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Orhun Dalabasmaz
 * @version 10.03.2021
 */

public class TerminateSqsResources implements TerminateResources {
    private static final Logger LOGGER = LogManager.getLogger(TerminateSqsResources.class);

    private final AWSCredentialsProvider credentialsProvider;

    public TerminateSqsResources(AWSCredentialsProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
    }

    @Override
    public void terminateResource(String region, String service, List<String> resources, String ticket, boolean apply) throws Exception {
        AmazonSQS sqsClient = AmazonSQSClient
                .builder()
                .withRegion(region)
                .withCredentials(credentialsProvider)
                .build();

        AmazonSNS snsClient = AmazonSNSClient
                .builder()
                .withRegion(region)
                .withCredentials(credentialsProvider)
                .build();

        AWSLambda lambdaClient = AWSLambdaClient
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
        LinkedHashSet<String> queuesToDelete = new LinkedHashSet<>();
        //LinkedHashSet<String> dlqsToDelete = new LinkedHashSet<>();
        LinkedHashSet<String> lambdaTriggersToDelete = new LinkedHashSet<>();
        LinkedHashSet<String> snsSubscriptionsToDelete = new LinkedHashSet<>();
        LinkedHashSet<String> cloudwatchAlarmsToDelete = new LinkedHashSet<>();

        List<String> details = new LinkedList<>();

        FetchResources fetcher = new FetchResourceFactory().getFetcher("sqs", credentialsProvider);
        List<SQSResource> sqsResourceList = (List<SQSResource>) fetcher.fetchResources(region, resources, details);
        for (SQSResource sqsResource : sqsResourceList) {
            queuesToDelete.add(sqsResource.getResourceName());
            lambdaTriggersToDelete.addAll(sqsResource.getLambdaTriggers());
            snsSubscriptionsToDelete.addAll(sqsResource.getSnsSubscriptions());
            cloudwatchAlarmsToDelete.addAll(sqsResource.getCloudwatchAlarms());
        }

        StringBuilder info = new StringBuilder()
                .append("The resources will be terminated regarding ").append(ticket).append("\n")
                .append("* Dry-Run: ").append(!apply).append("\n")
                .append("* Lambda event-source mappings: ").append(lambdaTriggersToDelete).append("\n")
                .append("* SNS subscriptions: ").append(snsSubscriptionsToDelete).append("\n")
                //.append("* DLQs: ").append(dlqsToDelete).append("\n")
                .append("* Queues: ").append(queuesToDelete).append("\n")
                .append("* Cloudwatch alarms: ").append(cloudwatchAlarmsToDelete).append("\n");

        info.append("Details:\n");
        details.forEach(d -> info.append("-- ").append(d).append("\n"));
        LOGGER.info(info);

        if (apply) {
            LOGGER.info("Terminating the resources...");

            lambdaTriggersToDelete.forEach(r -> lambdaClient.deleteEventSourceMapping(new DeleteEventSourceMappingRequest().withUUID(r)));

            snsSubscriptionsToDelete.forEach(snsClient::unsubscribe);

            if (!cloudwatchAlarmsToDelete.isEmpty()) {
                cloudWatchClient.deleteAlarms(new DeleteAlarmsRequest().withAlarmNames(cloudwatchAlarmsToDelete));
            }

            //dlqsToDelete.forEach(sqsClient::deleteQueue);

            queuesToDelete.forEach(sqsClient::deleteQueue);
        }

        LOGGER.info("Succeed.");
    }
}
