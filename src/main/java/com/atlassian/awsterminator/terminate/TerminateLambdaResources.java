package com.atlassian.awsterminator.terminate;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.policy.Policy;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.DeleteAlarmsRequest;
import com.amazonaws.services.cloudwatch.model.DescribeAlarmsRequest;
import com.amazonaws.services.cloudwatch.model.MetricAlarm;
import com.amazonaws.services.cloudwatchevents.AmazonCloudWatchEvents;
import com.amazonaws.services.cloudwatchevents.AmazonCloudWatchEventsClient;
import com.amazonaws.services.cloudwatchevents.model.DeleteRuleRequest;
import com.amazonaws.services.cloudwatchevents.model.ListTargetsByRuleRequest;
import com.amazonaws.services.cloudwatchevents.model.RemoveTargetsRequest;
import com.amazonaws.services.cloudwatchevents.model.Target;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClient;
import com.amazonaws.services.lambda.model.*;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Orhun Dalabasmaz
 * @version 10.03.2021
 */

public class TerminateLambdaResources implements TerminateResources {
    private static final Logger LOGGER = LogManager.getLogger(TerminateLambdaResources.class);

    private final AWSCredentialsProvider credentialsProvider;

    public TerminateLambdaResources(AWSCredentialsProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
    }

    @Override
    public void terminateResource(String region, String service, List<String> resources, String ticket, boolean apply) {
        // check triggers (sns, sqs, dynamodb stream)
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

        AmazonCloudWatchEvents cloudWatchEventsClient = AmazonCloudWatchEventsClient
                .builder()
                .withRegion(region)
                .withCredentials(credentialsProvider)
                .build();

        // Resources to be removed
        LinkedHashSet<String> lambdasToDelete = new LinkedHashSet<>();
        LinkedHashSet<String> cloudwatchAlarmsToDelete = new LinkedHashSet<>();
        LinkedHashSet<String> snsTriggersToDelete = new LinkedHashSet<>();
        LinkedHashSet<String> cloudwatchRulesToDelete = new LinkedHashSet<>();
        LinkedHashSet<String> cloudwatchRuleTargetsToDelete = new LinkedHashSet<>();
        LinkedHashSet<String> eventSourceMappingsToDelete = new LinkedHashSet<>();

        List<String> details = new LinkedList<>();

        // process each lambda
        for (String lambdaName : resources) {
            try {
                GetFunctionResult function = lambdaClient.getFunction(new GetFunctionRequest().withFunctionName(lambdaName));

                // dynamodb triggers
                List<EventSourceMappingConfiguration> eventSourceMappings = lambdaClient.listEventSourceMappings(new ListEventSourceMappingsRequest().withFunctionName(lambdaName)).getEventSourceMappings();
                eventSourceMappings.stream().map(EventSourceMappingConfiguration::getUUID).forEach(eventSourceMappingsToDelete::add);

                // sns & cw triggers
                Policy policy = Policy.fromJson(lambdaClient.getPolicy(new GetPolicyRequest().withFunctionName(lambdaName)).getPolicy());
                policy.getStatements().forEach(s -> {
                    String sourceArn = s.getConditions().get(0).getValues().get(0);
                    if (sourceArn.startsWith("arn:aws:sns:")) {
                        snsClient.listSubscriptionsByTopic(sourceArn).getSubscriptions().stream()
                                .filter(subs -> subs.getEndpoint().equals(function.getConfiguration().getFunctionArn()))
                                .findFirst().ifPresent(subs -> snsTriggersToDelete.add(subs.getSubscriptionArn()));
                    } else if (sourceArn.startsWith("arn:aws:events:")) {
                        String ruleName = sourceArn.split("/")[1];
                        List<Target> targets = cloudWatchEventsClient.listTargetsByRule(new ListTargetsByRuleRequest().withRule(ruleName)).getTargets();
                        targets.stream()
                                .filter(t -> t.getArn().equals(function.getConfiguration().getFunctionArn()))
                                .forEach(t -> cloudwatchRuleTargetsToDelete.add(ruleName + ":" + t.getId()));
                        if (targets.size() == 1) {
                            cloudwatchRulesToDelete.add(ruleName);
                        }
                    } else {
                        // unexpected trigger received
                        throw new UnsupportedOperationException("Unsupported trigger found: " + sourceArn);
                    }
                });

                // Cloudwatch alarms
                cloudWatchClient.describeAlarms(new DescribeAlarmsRequest().withAlarmNamePrefix(region + " " + lambdaName + " Lambda "))
                        .getMetricAlarms().stream().map(MetricAlarm::getAlarmName)
                        .forEach(cloudwatchAlarmsToDelete::add);

                // Add to delete list at last step if gathering the subscriptions fail
                lambdasToDelete.add(lambdaName);

                details.add(String.format("Resources info for: [%s], sns triggers: %s, event source mappings: %s, cw rules: %s, cw rule targets: %s, cw alarms: %s",
                        lambdaName, snsTriggersToDelete, eventSourceMappingsToDelete, cloudwatchRulesToDelete, cloudwatchRuleTargetsToDelete, cloudwatchAlarmsToDelete));

            } catch (ResourceNotFoundException ex) {
                details.add("!!! Lambda resource not exists: " + lambdaName);
                LOGGER.warn("Lambda resource not exists: " + lambdaName);
            } /*catch (Exception ex) {
                details.add("!!! Error occurred for: " + lambdaName);
                LOGGER.error("Error occurred while processing lambda: " + lambdaName, ex);
            }*/
        }

        StringBuilder info = new StringBuilder()
                .append("The resources will be terminated regarding ").append(ticket).append("\n")
                .append("* Dry-Run: ").append(!apply).append("\n")
                .append("* SNS triggers: ").append(snsTriggersToDelete).append("\n")
                .append("* Event source mappings (DynamoDB): ").append(eventSourceMappingsToDelete).append("\n")
                .append("* Cloudwatch rules: ").append(cloudwatchRulesToDelete).append("\n")
                .append("* Cloudwatch rule targets: ").append(cloudwatchRuleTargetsToDelete).append("\n")
                .append("* Lambdas: ").append(lambdasToDelete).append("\n")
                .append("* Cloudwatch alarms: ").append(cloudwatchAlarmsToDelete).append("\n");

        info.append("Details:\n");
        details.forEach(d -> info.append("-- ").append(d).append("\n"));
        LOGGER.info(info);

        if (apply) {
            LOGGER.info("Terminating the resources...");

            snsTriggersToDelete.forEach(snsClient::unsubscribe);

            eventSourceMappingsToDelete.forEach(id -> lambdaClient.deleteEventSourceMapping(new DeleteEventSourceMappingRequest().withUUID(id)));

            cloudwatchRuleTargetsToDelete.forEach(target -> {
                String ruleName = target.split(":")[0];
                String id = target.split(":")[1];
                cloudWatchEventsClient.removeTargets(new RemoveTargetsRequest().withRule(ruleName).withIds(id));
            });

            cloudwatchRulesToDelete.forEach(rule -> cloudWatchEventsClient.deleteRule(new DeleteRuleRequest().withName(rule)));

            if (!cloudwatchAlarmsToDelete.isEmpty()) {
                cloudWatchClient.deleteAlarms(new DeleteAlarmsRequest().withAlarmNames(cloudwatchAlarmsToDelete));
            }

            lambdasToDelete.forEach(l -> lambdaClient.deleteFunction(new DeleteFunctionRequest().withFunctionName(l)));
        }

        LOGGER.info("Succeed.");
    }

}
