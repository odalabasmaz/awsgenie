package com.atlassian.awstool.terminate.lambda;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.policy.Policy;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.DescribeAlarmsRequest;
import com.amazonaws.services.cloudwatch.model.MetricAlarm;
import com.amazonaws.services.cloudwatchevents.AmazonCloudWatchEvents;
import com.amazonaws.services.cloudwatchevents.AmazonCloudWatchEventsClient;
import com.amazonaws.services.cloudwatchevents.model.ListTargetsByRuleRequest;
import com.amazonaws.services.cloudwatchevents.model.Target;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClient;
import com.amazonaws.services.lambda.model.*;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.atlassian.awstool.terminate.FetchResources;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.function.Consumer;

/**
 * @author Orhun Dalabasmaz
 * @version 10.03.2021
 */

public class FetchLambdaResources implements FetchResources<LambdaResource> {
    private static final Logger LOGGER = LogManager.getLogger(FetchLambdaResources.class);

    private final AWSCredentialsProvider credentialsProvider;
    private final Map<String, AWSLambda> lambdaClientMap;

    public FetchLambdaResources(AWSCredentialsProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
        this.lambdaClientMap = new HashMap<>();
    }

    @Override
    public List<LambdaResource> fetchResources(String region, List<String> resources, List<String> details) {
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
        List<LambdaResource> lambdaResourceList = new ArrayList<>();

        // process each lambda
        for (String lambdaName : resources) {
            try {
                LinkedHashSet<String> cloudwatchAlarmsToDelete = new LinkedHashSet<>();
                LinkedHashSet<String> snsTriggersToDelete = new LinkedHashSet<>();
                LinkedHashSet<String> cloudwatchRulesToDelete = new LinkedHashSet<>();
                LinkedHashSet<String> cloudwatchRuleTargetsToDelete = new LinkedHashSet<>();
                LinkedHashSet<String> eventSourceMappingsToDelete = new LinkedHashSet<>();

                GetFunctionResult function = lambdaClient.getFunction(new GetFunctionRequest().withFunctionName(lambdaName));

                // dynamodb triggers
                List<EventSourceMappingConfiguration> eventSourceMappings = lambdaClient.listEventSourceMappings(new ListEventSourceMappingsRequest().withFunctionName(lambdaName)).getEventSourceMappings();
                eventSourceMappings.stream().map(EventSourceMappingConfiguration::getUUID).forEach(eventSourceMappingsToDelete::add);

                // sns & cw triggers
                try {
                    Policy policy = Policy.fromJson(lambdaClient.getPolicy(new GetPolicyRequest().withFunctionName(lambdaName)).getPolicy());
                    policy.getStatements().forEach(s -> {
                        String sourceArn = s.getConditions().get(0).getValues().get(0);
                        if (sourceArn.startsWith("arn:aws:sns:")) {
                            try {
                                snsClient.listSubscriptionsByTopic(sourceArn).getSubscriptions().stream()
                                        .filter(subs -> subs.getEndpoint().equals(function.getConfiguration().getFunctionArn()))
                                        .findFirst().ifPresent(subs -> snsTriggersToDelete.add(subs.getSubscriptionArn()));
                            } catch (com.amazonaws.services.sns.model.NotFoundException ex) {
                                LOGGER.warn("Topic seems already deleted: " + sourceArn);
                            }
                        } else if (sourceArn.startsWith("arn:aws:events:")) {
                            try {
                                String ruleName = sourceArn.split("/")[1];
                                List<Target> targets = cloudWatchEventsClient.listTargetsByRule(new ListTargetsByRuleRequest().withRule(ruleName)).getTargets();
                                targets.stream()
                                        .filter(t -> t.getArn().equals(function.getConfiguration().getFunctionArn()))
                                        .forEach(t -> cloudwatchRuleTargetsToDelete.add(ruleName + ":" + t.getId()));
                                if (targets.size() == 1) {
                                    cloudwatchRulesToDelete.add(ruleName);
                                }
                            } catch (com.amazonaws.services.cloudwatchevents.model.ResourceNotFoundException ex) {
                                LOGGER.warn("CW trigger seems already deleted: " + sourceArn);
                            }
                        } else {
                            // unexpected trigger received
                            //TODO: check if there is another option that we should consider...
                            throw new UnsupportedOperationException("Unsupported trigger found: " + sourceArn);
                        }
                    });
                } catch (Exception ex) {
                    LOGGER.warn("ex.getMessage()" + ex.getMessage());
                }

                // Cloudwatch alarms
                //TODO: convention is not same for all developers (lambdaName + Lambda), should be generic..
                cloudWatchClient.describeAlarms(new DescribeAlarmsRequest().withAlarmNamePrefix(region + " " + lambdaName + " Lambda "))
                        .getMetricAlarms().stream().map(MetricAlarm::getAlarmName)
                        .forEach(cloudwatchAlarmsToDelete::add);
                // Add to delete list at last step if gathering the subscriptions fail
                LambdaResource lambdaResource = new LambdaResource()
                        .setCloudwatchAlarms(cloudwatchAlarmsToDelete)
                        .setSnsTriggers(snsTriggersToDelete)
                        .setCloudwatchRules(cloudwatchRulesToDelete)
                        .setCloudwatchRuleTargets(cloudwatchRuleTargetsToDelete)
                        .setEventSourceMappings(eventSourceMappingsToDelete)
                        .setResourceName(lambdaName);

                lambdaResourceList.add(lambdaResource);
                details.add(String.format("Resources info for: [%s], sns triggers: %s, event source mappings: %s, cw rules: %s, cw rule targets: %s, cw alarms: %s",
                        lambdaName, snsTriggersToDelete, eventSourceMappingsToDelete, cloudwatchRulesToDelete, cloudwatchRuleTargetsToDelete, cloudwatchAlarmsToDelete));

            } catch (ResourceNotFoundException ex) {
                details.add("!!! Lambda resource not exists: " + lambdaName);
                LOGGER.warn("Lambda resource not exists: " + lambdaName);
                LOGGER.warn("ex.getMessage()" + ex.getMessage());
            }
        }

        LOGGER.info("Succeed.");

        return lambdaResourceList;
    }

    @Override
    public void listResources(String region, Consumer<List<String>> consumer) {

        List<String> lambdaResourceNameList = new ArrayList<>();

        consume((String nextMarker) -> {
            ListFunctionsResult listFunctionsResult = getLambdaClient(region).listFunctions(new ListFunctionsRequest().withMarker(nextMarker));
            for (FunctionConfiguration functionConfiguration : listFunctionsResult.getFunctions()) {
                lambdaResourceNameList.add(functionConfiguration.getFunctionName());
            }

            consumer.accept(lambdaResourceNameList);
            return listFunctionsResult.getNextMarker();
        });
    }

    private AWSLambda getLambdaClient(String region) {
        if (lambdaClientMap.get(region) == null) {
            lambdaClientMap.put(region, AWSLambdaClient
                    .builder()
                    .withRegion(region)
                    .withCredentials(credentialsProvider)
                    .build());
        }
        return lambdaClientMap.get(region);
    }
}
