package com.atlassian.awsterminator.terminate;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.DeleteAlarmsRequest;
import com.amazonaws.services.cloudwatchevents.AmazonCloudWatchEvents;
import com.amazonaws.services.cloudwatchevents.model.DeleteRuleRequest;
import com.amazonaws.services.cloudwatchevents.model.RemoveTargetsRequest;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.DeleteEventSourceMappingRequest;
import com.amazonaws.services.lambda.model.DeleteFunctionRequest;
import com.amazonaws.services.sns.AmazonSNS;
import com.atlassian.awsterminator.configuration.Configuration;
import com.atlassian.awsterminator.interceptor.InterceptorRegistry;
import com.atlassian.awstool.terminate.FetchResourceFactory;
import com.atlassian.awstool.terminate.FetchResources;
import com.atlassian.awstool.terminate.FetcherConfiguration;
import com.atlassian.awstool.terminate.Service;
import com.atlassian.awstool.terminate.lambda.LambdaResource;
import credentials.AwsClientConfiguration;
import credentials.AwsClientProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Orhun Dalabasmaz
 * @version 10.03.2021
 */

public class TerminateLambdaResources extends TerminateResourcesWithProvider implements TerminateResources<LambdaResource> {
    private static final Logger LOGGER = LogManager.getLogger(TerminateLambdaResources.class);

    private FetchResourceFactory<LambdaResource> fetchResourceFactory;

    public TerminateLambdaResources(AwsClientConfiguration configuration) {
        super(configuration);
    }

    @Override
    public void terminateResource(Configuration conf, boolean apply) throws Exception {
        terminateResource(conf.getRegion(), Service.fromValue(conf.getService()), conf.getResourcesAsList(), conf.getDescription(),
                conf.getLastUsage(), conf.isForce(), apply);
    }

    @Override
    public void terminateResource(String region, Service service, List<String> resources, String ticket, boolean apply) throws Exception {
        terminateResource(region, service, resources, ticket, 7, false, apply);
    }

    @Override
    public void terminateResource(String region, Service service, List<String> resources, String ticket, int lastUsage, boolean force, boolean apply) throws Exception {
        // check triggers (sns, sqs, dynamodb stream)
        AmazonSNS snsClient = AwsClientProvider.getInstance(getConfiguration()).getAmazonSNS();

        AWSLambda lambdaClient = AwsClientProvider.getInstance(getConfiguration()).getAmazonLambda();

        AmazonCloudWatch cloudWatchClient = AwsClientProvider.getInstance(getConfiguration()).getAmazonCloudWatch();

        AmazonCloudWatchEvents cloudWatchEventsClient = AwsClientProvider.getInstance(getConfiguration()).getAmazonCloudWatchEvents();

        // Resources to be removed
        LinkedHashSet<String> lambdasToDelete = new LinkedHashSet<>();
        LinkedHashSet<String> cloudwatchAlarmsToDelete = new LinkedHashSet<>();
        LinkedHashSet<String> snsTriggersToDelete = new LinkedHashSet<>();
        LinkedHashSet<String> cloudwatchRulesToDelete = new LinkedHashSet<>();
        LinkedHashSet<String> cloudwatchRuleTargetsToDelete = new LinkedHashSet<>();
        LinkedHashSet<String> eventSourceMappingsToDelete = new LinkedHashSet<>();

        List<String> details = new LinkedList<>();

        FetchResources<LambdaResource> fetchResources = getFetchResourceFactory().getFetcher(service, new FetcherConfiguration(getConfiguration()));
        List<LambdaResource> lambdaResourceList = fetchResources.fetchResources(region, resources, details);

        for (LambdaResource lambdaResource : lambdaResourceList) {
            lambdasToDelete.add(lambdaResource.getResourceName());
            cloudwatchAlarmsToDelete.addAll(lambdaResource.getCloudwatchAlarms());
            snsTriggersToDelete.addAll(lambdaResource.getSnsTriggers());
            cloudwatchRulesToDelete.addAll(lambdaResource.getCloudwatchRules());
            cloudwatchRuleTargetsToDelete.addAll(lambdaResource.getCloudwatchRuleTargets());
            eventSourceMappingsToDelete.addAll(lambdaResource.getEventSourceMappings());
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

        InterceptorRegistry.getBeforeTerminateInterceptors()
                .forEach(interceptor -> interceptor.intercept(service, lambdaResourceList, info.toString(), apply));

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

        InterceptorRegistry.getAfterTerminateInterceptors()
                .forEach(interceptor -> interceptor.intercept(service, lambdaResourceList, info.toString(), apply));

        LOGGER.info("Succeed.");
    }

    void setFetchResourceFactory(FetchResourceFactory<LambdaResource> fetchResourceFactory) {
        this.fetchResourceFactory = fetchResourceFactory;
    }

    private FetchResourceFactory<LambdaResource> getFetchResourceFactory() {
        if (this.fetchResourceFactory != null) {
            return this.fetchResourceFactory;
        } else {
            return new FetchResourceFactory<>();
        }
    }
}
