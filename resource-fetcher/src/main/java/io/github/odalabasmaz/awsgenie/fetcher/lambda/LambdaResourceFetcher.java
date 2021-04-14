package io.github.odalabasmaz.awsgenie.fetcher.lambda;

import com.amazonaws.auth.policy.Policy;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.DescribeAlarmsRequest;
import com.amazonaws.services.cloudwatch.model.MetricAlarm;
import com.amazonaws.services.cloudwatchevents.AmazonCloudWatchEvents;
import com.amazonaws.services.cloudwatchevents.model.ListTargetsByRuleRequest;
import com.amazonaws.services.cloudwatchevents.model.Target;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.*;
import com.amazonaws.services.sns.AmazonSNS;
import io.github.odalabasmaz.awsgenie.fetcher.ResourceFetcher;
import io.github.odalabasmaz.awsgenie.fetcher.ResourceFetcherConfiguration;
import io.github.odalabasmaz.awsgenie.fetcher.ResourceFetcherWithProvider;
import io.github.odalabasmaz.awsgenie.fetcher.credentials.AWSClientProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author Orhun Dalabasmaz
 * @version 10.03.2021
 */

public class LambdaResourceFetcher extends ResourceFetcherWithProvider implements ResourceFetcher<LambdaResource> {
    private static final Logger LOGGER = LogManager.getLogger(LambdaResourceFetcher.class);


    public LambdaResourceFetcher(ResourceFetcherConfiguration configuration) {
        super(configuration);
    }

    @Override
    public List<LambdaResource> fetchResources(String region, List<String> resources, List<String> details) {
        // check triggers (sns, sqs, dynamodb stream)
        AmazonSNS snsClient = AWSClientProvider.getInstance(getConfiguration()).getAmazonSNS();
        AWSLambda lambdaClient = AWSClientProvider.getInstance(getConfiguration()).getAmazonLambda();
        AmazonCloudWatch cloudWatchClient = AWSClientProvider.getInstance(getConfiguration()).getAmazonCloudWatch();
        AmazonCloudWatchEvents cloudWatchEventsClient = AWSClientProvider.getInstance(getConfiguration()).getAmazonCloudWatchEvents();

        // Resources to be removed
        List<LambdaResource> lambdaResourceList = new ArrayList<>();

        // process each lambda
        for (String lambdaName : resources) {
            LOGGER.info("Processing for lambda: [" + lambdaName + "]");
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
                            //TODO: check if there is another option that we should consider: api-gw
                            //TODO: what to do if there is a subscription?
                            LOGGER.warn("Unsupported trigger found: " + sourceArn);
                        }
                    });
                } catch (ResourceNotFoundException ex) {
                    LOGGER.info("Lambda policy not exists: " + lambdaName);
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

                LOGGER.info("Process successfully completed for lambda: [" + lambdaName + "]");
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
        consume((String nextMarker) -> {
            List<String> lambdaResourceNameList = new ArrayList<>();
            ListFunctionsResult listFunctionsResult = AWSClientProvider.getInstance(getConfiguration()).getAmazonLambda().listFunctions(new ListFunctionsRequest().withMarker(nextMarker));
            for (FunctionConfiguration functionConfiguration : listFunctionsResult.getFunctions()) {
                lambdaResourceNameList.add(functionConfiguration.getFunctionName());
            }

            consumer.accept(lambdaResourceNameList);
            return listFunctionsResult.getNextMarker();
        });
    }
}
