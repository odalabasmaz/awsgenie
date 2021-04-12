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
import com.atlassian.awstool.terminate.AWSResource;
import com.atlassian.awstool.terminate.FetchResources;
import com.atlassian.awstool.terminate.FetchResourcesWithProvider;
import com.atlassian.awstool.terminate.FetcherConfiguration;
import credentials.AwsClientProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.security.auth.login.Configuration;
import java.util.*;
import java.util.function.Consumer;

/**
 * @author Orhun Dalabasmaz
 * @version 10.03.2021
 */

public class FetchLambdaResources extends FetchResourcesWithProvider implements FetchResources<LambdaResource> {
    private static final Logger LOGGER = LogManager.getLogger(FetchLambdaResources.class);


    public FetchLambdaResources(FetcherConfiguration configuration) {
        super(configuration);
    }


    @Override
    public List<LambdaResource> fetchResources(String region, List<String> resources, List<String> details) {
        // check triggers (sns, sqs, dynamodb stream)
        AmazonSNS snsClient = AwsClientProvider.getInstance(getConfiguration()).getAmazonSNS();
        AWSLambda lambdaClient = AwsClientProvider.getInstance(getConfiguration()).getAmazonLambda();
        AmazonCloudWatch cloudWatchClient = AwsClientProvider.getInstance(getConfiguration()).getAmazonCloudWatch();
        AmazonCloudWatchEvents cloudWatchEventsClient = AwsClientProvider.getInstance(getConfiguration()).getAmazonCloudWatchEvents();

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
                    Policy policy = policy = Policy.fromJson(lambdaClient.getPolicy(new GetPolicyRequest().withFunctionName(lambdaName)).getPolicy());
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
                } catch (Exception ex) {
                    LOGGER.warn("ex.getMessage()" + ex.getMessage());
                }

                // Cloudwatch alarms
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
            ListFunctionsResult listFunctionsResult = AwsClientProvider.getInstance(getConfiguration()).getAmazonLambda().listFunctions(new ListFunctionsRequest().withMarker(nextMarker));
            for (FunctionConfiguration functionConfiguration : listFunctionsResult.getFunctions()) {
                lambdaResourceNameList.add(functionConfiguration.getFunctionName());
            }

            consumer.accept(lambdaResourceNameList);
            return listFunctionsResult.getNextMarker();
        });
    }
}
