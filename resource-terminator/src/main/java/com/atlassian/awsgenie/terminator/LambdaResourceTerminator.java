package com.atlassian.awsgenie.terminator;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.DeleteAlarmsRequest;
import com.amazonaws.services.cloudwatchevents.AmazonCloudWatchEvents;
import com.amazonaws.services.cloudwatchevents.model.DeleteRuleRequest;
import com.amazonaws.services.cloudwatchevents.model.RemoveTargetsRequest;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.DeleteEventSourceMappingRequest;
import com.amazonaws.services.lambda.model.DeleteFunctionRequest;
import com.amazonaws.services.sns.AmazonSNS;
import com.atlassian.awsgenie.fetcher.ResourceFetcher;
import com.atlassian.awsgenie.fetcher.ResourceFetcherConfiguration;
import com.atlassian.awsgenie.fetcher.ResourceFetcherFactory;
import com.atlassian.awsgenie.fetcher.Service;
import com.atlassian.awsgenie.fetcher.credentials.AWSClientConfiguration;
import com.atlassian.awsgenie.fetcher.credentials.AWSClientProvider;
import com.atlassian.awsgenie.fetcher.lambda.LambdaResource;
import com.atlassian.awsgenie.terminator.configuration.Configuration;
import com.atlassian.awsgenie.terminator.interceptor.InterceptorRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Orhun Dalabasmaz
 * @version 10.03.2021
 */

public class LambdaResourceTerminator extends ResourceTerminatorWithProvider implements ResourceTerminator<LambdaResource> {
    private static final Logger LOGGER = LogManager.getLogger(LambdaResourceTerminator.class);

    private ResourceFetcherFactory<LambdaResource> resourceFetcherFactory;

    public LambdaResourceTerminator(AWSClientConfiguration configuration) {
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
        AmazonSNS snsClient = AWSClientProvider.getInstance(getConfiguration()).getAmazonSNS();

        AWSLambda lambdaClient = AWSClientProvider.getInstance(getConfiguration()).getAmazonLambda();

        AmazonCloudWatch cloudWatchClient = AWSClientProvider.getInstance(getConfiguration()).getAmazonCloudWatch();

        AmazonCloudWatchEvents cloudWatchEventsClient = AWSClientProvider.getInstance(getConfiguration()).getAmazonCloudWatchEvents();

        // Resources to be removed
        LinkedHashSet<String> lambdasToDelete = new LinkedHashSet<>();
        LinkedHashSet<String> cloudwatchAlarmsToDelete = new LinkedHashSet<>();
        LinkedHashSet<String> snsTriggersToDelete = new LinkedHashSet<>();
        LinkedHashSet<String> cloudwatchRulesToDelete = new LinkedHashSet<>();
        LinkedHashSet<String> cloudwatchRuleTargetsToDelete = new LinkedHashSet<>();
        LinkedHashSet<String> eventSourceMappingsToDelete = new LinkedHashSet<>();

        List<String> details = new LinkedList<>();

        ResourceFetcher<LambdaResource> resourceFetcher = getFetchResourceFactory().getFetcher(service, new ResourceFetcherConfiguration(getConfiguration()));
        List<LambdaResource> lambdaResourceList = resourceFetcher.fetchResources(region, resources, details);

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

    void setFetchResourceFactory(ResourceFetcherFactory<LambdaResource> resourceFetcherFactory) {
        this.resourceFetcherFactory = resourceFetcherFactory;
    }

    private ResourceFetcherFactory<LambdaResource> getFetchResourceFactory() {
        if (this.resourceFetcherFactory != null) {
            return this.resourceFetcherFactory;
        } else {
            return new ResourceFetcherFactory<>();
        }
    }
}
