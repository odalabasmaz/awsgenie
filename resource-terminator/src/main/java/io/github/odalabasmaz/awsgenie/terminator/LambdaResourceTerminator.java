package io.github.odalabasmaz.awsgenie.terminator;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.DeleteAlarmsRequest;
import com.amazonaws.services.cloudwatchevents.AmazonCloudWatchEvents;
import com.amazonaws.services.cloudwatchevents.model.DeleteRuleRequest;
import com.amazonaws.services.cloudwatchevents.model.RemoveTargetsRequest;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.DeleteEventSourceMappingRequest;
import com.amazonaws.services.lambda.model.DeleteFunctionRequest;
import com.amazonaws.services.sns.AmazonSNS;
import io.github.odalabasmaz.awsgenie.fetcher.ResourceFetcher;
import io.github.odalabasmaz.awsgenie.fetcher.ResourceFetcherConfiguration;
import io.github.odalabasmaz.awsgenie.fetcher.ResourceFetcherFactory;
import io.github.odalabasmaz.awsgenie.fetcher.Service;
import io.github.odalabasmaz.awsgenie.fetcher.credentials.AWSClientConfiguration;
import io.github.odalabasmaz.awsgenie.fetcher.credentials.AWSClientProvider;
import io.github.odalabasmaz.awsgenie.fetcher.lambda.LambdaResource;
import io.github.odalabasmaz.awsgenie.terminator.configuration.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * @author Orhun Dalabasmaz
 * @version 10.03.2021
 */

public class LambdaResourceTerminator extends ResourceTerminator<LambdaResource> {
    private static final Logger LOGGER = LogManager.getLogger(LambdaResourceTerminator.class);

    public LambdaResourceTerminator(AWSClientConfiguration configuration) {
        super(configuration);
    }

    @Override
    public Set<LambdaResource> beforeApply(Configuration conf, boolean apply) throws Exception {
        return beforeApply(conf.getRegion(), Service.fromValue(conf.getService()), conf.getResourcesAsList(), conf.getDescription(),
                conf.getLastUsage(), conf.isForce(), apply);
    }

    @Override
    protected Set<LambdaResource> beforeApply(String region, Service service, List<String> resources, String ticket, boolean apply) throws Exception {
        return beforeApply(region, service, resources, ticket, 7, false, apply);
    }

    @Override
    protected Set<LambdaResource> beforeApply(String region, Service service, List<String> resources, String ticket, int lastUsage, boolean force, boolean apply) throws Exception {
        // Resources to be removed
        List<String> details = new LinkedList<>();
        ResourceFetcher<LambdaResource> resourceFetcher = getFetchResourceFactory().getFetcher(service, new ResourceFetcherConfiguration(getConfiguration()));
        Set<LambdaResource> lambdasToDelete = resourceFetcher.fetchResources(region, resources, details);

        StringBuilder info = new StringBuilder()
                .append("The resources will be terminated regarding ").append(ticket).append("\n")
                .append("* Dry-Run: ").append(!apply).append("\n")
                .append("* Lambdas: ").append(lambdasToDelete).append("\n");

        info.append("Details:\n");
        details.forEach(d -> info.append("-- ").append(d).append("\n"));
        LOGGER.info(info);

        return lambdasToDelete;
    }

    @Override
    protected void apply(Set<LambdaResource> resources, boolean apply) {
        List<String> details = new ArrayList<>();
        Set<String> lambdasToDelete = new LinkedHashSet<>();
        Set<String> cloudwatchAlarmsToDelete = new LinkedHashSet<>();
        Set<String> snsTriggersToDelete = new LinkedHashSet<>();
        Set<String> cloudwatchRulesToDelete = new LinkedHashSet<>();
        Set<String> cloudwatchRuleTargetsToDelete = new LinkedHashSet<>();
        Set<String> eventSourceMappingsToDelete = new LinkedHashSet<>();

        resources.forEach(resource -> {
            lambdasToDelete.add(resource.getResourceName());
            cloudwatchAlarmsToDelete.addAll(resource.getCloudwatchAlarms());
            snsTriggersToDelete.addAll(resource.getSnsTriggers());
            cloudwatchRulesToDelete.addAll(resource.getCloudwatchRules());
            cloudwatchRuleTargetsToDelete.addAll(resource.getCloudwatchRuleTargets());
            eventSourceMappingsToDelete.addAll(resource.getEventSourceMappings());
        });

        details.add("SNS triggers: " + snsTriggersToDelete);
        details.add("Event source mappings (DynamoDB): " + eventSourceMappingsToDelete);
        details.add("Cloudwatch rules: " + cloudwatchRulesToDelete);
        details.add("Cloudwatch rule targets: " + cloudwatchRuleTargetsToDelete);
        details.add("Cloudwatch alarms: " + cloudwatchAlarmsToDelete);

        StringBuilder info = new StringBuilder();
        details.forEach(d -> info.append("-- ").append(d).append("\n"));
        LOGGER.info(info);

        if (!resources.isEmpty() && apply) {
            AmazonSNS snsClient = AWSClientProvider.getInstance(getConfiguration()).getAmazonSNS();
            AWSLambda lambdaClient = AWSClientProvider.getInstance(getConfiguration()).getAmazonLambda();
            AmazonCloudWatch cloudWatchClient = AWSClientProvider.getInstance(getConfiguration()).getAmazonCloudWatch();
            AmazonCloudWatchEvents cloudWatchEventsClient = AWSClientProvider.getInstance(getConfiguration()).getAmazonCloudWatchEvents();

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
    }

    @Override
    protected void afterApply(Set<LambdaResource> resources) {
        LOGGER.info("Succeed.");
    }

    void setFetchResourceFactory(ResourceFetcherFactory<LambdaResource> resourceFetcherFactory) {
        this.resourceFetcherFactory = resourceFetcherFactory;
    }
}
