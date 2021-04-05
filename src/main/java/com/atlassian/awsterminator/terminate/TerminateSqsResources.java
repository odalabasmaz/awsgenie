package com.atlassian.awsterminator.terminate;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.DeleteAlarmsRequest;
import com.amazonaws.services.cloudwatch.model.DescribeAlarmsRequest;
import com.amazonaws.services.cloudwatch.model.MetricAlarm;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClient;
import com.amazonaws.services.lambda.model.DeleteEventSourceMappingRequest;
import com.amazonaws.services.lambda.model.EventSourceMappingConfiguration;
import com.amazonaws.services.lambda.model.ListEventSourceMappingsRequest;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.ListSubscriptionsRequest;
import com.amazonaws.services.sns.model.ListSubscriptionsResult;
import com.amazonaws.services.sns.model.Subscription;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlRequest;
import com.amazonaws.services.sqs.model.QueueDoesNotExistException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

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
    public void terminateResource(String region, String service, List<String> resources, String ticket, boolean apply) {
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

        // build sns subscription - sqs name mapping
        Map<String, List<String>> sqsSnsMapping = new HashMap<>();
        String nextToken = null;
        do {
            ListSubscriptionsResult result = snsClient.listSubscriptions(
                    new ListSubscriptionsRequest().withNextToken(nextToken));
            nextToken = result.getNextToken();

            for (Subscription subs : result.getSubscriptions()) {
                String protocol = subs.getProtocol();
                String endpoint = subs.getEndpoint();
                if (!protocol.equals("sqs") || endpoint.contains("_cmd_")) {
                    continue;
                }

                //String snsName = subs.getTopicArn().split(":")[5];
                String queueName = endpoint.split(":")[5];
                if (!resources.contains(queueName)) {
                    continue;
                }

                String subscriptionArn = subs.getSubscriptionArn();
                List<String> snsList = sqsSnsMapping.computeIfAbsent(queueName, k -> new ArrayList<>());
                snsList.add(subscriptionArn);

                if (sqsSnsMapping.size() == resources.size()) {
                    // we found all we need
                    nextToken = null;
                    break;
                }
            }
        } while (nextToken != null);

        // process each queue
        for (String queueName : resources) {
            try {
                // QUEUE
                String queueUrl = sqsClient.getQueueUrl(new GetQueueUrlRequest().withQueueName(queueName)).getQueueUrl();
                Map<String, String> attributes = sqsClient.getQueueAttributes(new GetQueueAttributesRequest().withQueueUrl(queueUrl).withAttributeNames("All")).getAttributes();
                String numberOfMessages = attributes.get("ApproximateNumberOfMessages");
                String queueArn = attributes.get("QueueArn");
                //String dlqUrlArn = attributes.get("RedrivePolicy") != null ? JSON.parse(attributes.get("RedrivePolicy")).get("deadLetterTargetArn").toString() : null;

                // Cloudwatch alarms
                List<String> cwAlarms = cloudWatchClient.describeAlarms(new DescribeAlarmsRequest().withAlarmNamePrefix("SQS Queue " + queueName + " "))
                        .getMetricAlarms().stream().map(MetricAlarm::getAlarmName)
                        .collect(Collectors.toList());

                // DLQ (commented out since multiple queues can use the same queue as DLQ)
                /*
                String dlqNumberOfMessages = "0";
                if (dlqUrlArn != null) {
                    String dlqName = dlqUrlArn.split(":")[5];
                    String dlqUrl = sqsClient.getQueueUrl(new GetQueueUrlRequest().withQueueName(dlqName)).getQueueUrl();
                    Map<String, String> dlqAttributes = sqsClient.getQueueAttributes(new GetQueueAttributesRequest().withQueueUrl(dlqUrl).withAttributeNames("All")).getAttributes();
                    dlqNumberOfMessages = dlqAttributes.get("ApproximateNumberOfMessages");

                    // Cloudwatch alarms
                    cloudWatchClient.describeAlarms(new DescribeAlarmsRequest().withAlarmNamePrefix("SQS Queue " + dlqName + " "))
                            .getMetricAlarms().stream().map(MetricAlarm::getAlarmName)
                            .forEach(cloudwatchAlarmsToDelete::add);
                }
                */

                // SNS subscriptions
                List<String> snsSubscriptionArns = sqsSnsMapping.get(queueName);
                if (snsSubscriptionArns == null) snsSubscriptionArns = Collections.emptyList();

                // Lambda triggers
                ListEventSourceMappingsRequest listEventSourceMappingsRequest = new ListEventSourceMappingsRequest();
                listEventSourceMappingsRequest.putCustomQueryParameter("EventSourceArn", queueArn);
                List<EventSourceMappingConfiguration> eventSourceMappings = lambdaClient.listEventSourceMappings(listEventSourceMappingsRequest).getEventSourceMappings();
                List<String> functions = eventSourceMappings.stream().map(EventSourceMappingConfiguration::getFunctionArn).map(arn -> arn.split(":")[6]).collect(Collectors.toList());
                List<String> eventSourceIds = eventSourceMappings.stream().map(EventSourceMappingConfiguration::getUUID).collect(Collectors.toList());

                // Add to delete list at last step if gathering the subscriptions fail
                queuesToDelete.add(queueUrl);
                //dlqsToDelete.add(dlqUrl);
                cloudwatchAlarmsToDelete.addAll(cwAlarms);
                snsSubscriptionsToDelete.addAll(snsSubscriptionArns);
                lambdaTriggersToDelete.addAll(eventSourceIds);

                details.add(String.format("Resources info for: [%s], there are [%s] message(s) in queue, sns subscription(s): %s, lambda trigger(s): %s, cw alarms: %s",
                        queueName, numberOfMessages, snsSubscriptionArns, functions, cloudwatchAlarmsToDelete));

            } catch (QueueDoesNotExistException ex) {
                details.add("!!! SQS resource not exists: " + queueName);
                LOGGER.warn("SQS resource not exists: " + queueName);
            } /*catch (Exception ex) {
                details.add("!!! Error occurred for: " + queueName);
                LOGGER.error("Error occurred while processing queue: " + queueName, ex);
            }*/
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
