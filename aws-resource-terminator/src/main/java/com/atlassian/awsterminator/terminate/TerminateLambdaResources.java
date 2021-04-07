package com.atlassian.awsterminator.terminate;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.DeleteAlarmsRequest;
import com.amazonaws.services.cloudwatchevents.AmazonCloudWatchEvents;
import com.amazonaws.services.cloudwatchevents.AmazonCloudWatchEventsClient;
import com.amazonaws.services.cloudwatchevents.model.DeleteRuleRequest;
import com.amazonaws.services.cloudwatchevents.model.RemoveTargetsRequest;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClient;
import com.amazonaws.services.lambda.model.DeleteEventSourceMappingRequest;
import com.amazonaws.services.lambda.model.DeleteFunctionRequest;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.atlassian.awstool.terminate.FetchResourceFactory;
import com.atlassian.awstool.terminate.FetchResources;
import com.atlassian.awstool.terminate.lambda.LambdaResource;
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
    public void terminateResource(String region, String service, List<String> resources, String ticket, boolean apply) throws Exception {
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

        FetchResourceFactory fetchResourceFactory = new FetchResourceFactory();
        FetchResources fetchResources = fetchResourceFactory.getFetcher("lambda", credentialsProvider);
        List<LambdaResource> lambdaResourceList = (List<LambdaResource>) fetchResources.fetchResources(region, resources, details);

        for (LambdaResource lambdaResource : lambdaResourceList) {
            lambdasToDelete.add(lambdaResource.getResourceName());
            cloudwatchAlarmsToDelete.addAll(lambdaResource.getCloudwatchAlarmsToDelete());
            snsTriggersToDelete.addAll(lambdaResource.getSnsTriggersToDelete());
            cloudwatchRulesToDelete.addAll(lambdaResource.getCloudwatchRulesToDelete());
            cloudwatchRuleTargetsToDelete.addAll(lambdaResource.getCloudwatchRuleTargetsToDelete());
            eventSourceMappingsToDelete.addAll(lambdaResource.getCloudwatchRuleTargetsToDelete());
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
