package io.github.odalabasmaz.awsgenie.terminator;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.DeleteAlarmsRequest;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.DeleteEventSourceMappingRequest;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sqs.AmazonSQS;
import io.github.odalabasmaz.awsgenie.fetcher.ResourceFetcher;
import io.github.odalabasmaz.awsgenie.fetcher.ResourceFetcherConfiguration;
import io.github.odalabasmaz.awsgenie.fetcher.ResourceFetcherFactory;
import io.github.odalabasmaz.awsgenie.fetcher.Service;
import io.github.odalabasmaz.awsgenie.fetcher.credentials.AWSClientConfiguration;
import io.github.odalabasmaz.awsgenie.fetcher.credentials.AWSClientProvider;
import io.github.odalabasmaz.awsgenie.fetcher.sqs.SQSResource;
import io.github.odalabasmaz.awsgenie.terminator.configuration.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * @author Orhun Dalabasmaz
 * @version 10.03.2021
 */

public class SQSResourceTerminator extends ResourceTerminator<SQSResource> {
    private static final Logger LOGGER = LogManager.getLogger(SQSResourceTerminator.class);

    public SQSResourceTerminator(AWSClientConfiguration configuration) {
        super(configuration);
    }

    @Override
    public Set<SQSResource> beforeApply(Configuration conf, boolean apply) throws Exception {
        return beforeApply(conf.getRegion(), Service.fromValue(conf.getService()), conf.getResourcesAsList(), conf.getDescription(),
                conf.getLastUsage(), conf.isForce(), apply);
    }

    @Override
    protected Set<SQSResource> beforeApply(String region, Service service, List<String> resources, String ticket, boolean apply) throws Exception {
        return beforeApply(region, service, resources, ticket, 7, false, apply);
    }

    @Override
    protected Set<SQSResource> beforeApply(String region, Service service, List<String> resources, String ticket, int lastUsage, boolean force, boolean apply) throws Exception {
        // Resources to be removed
        Set<SQSResource> queuesToDelete = new LinkedHashSet<>();
        List<String> details = new LinkedList<>();

        ResourceFetcher<SQSResource> fetcher = getFetchResourceFactory().getFetcher(service, new ResourceFetcherConfiguration(getConfiguration()));
        Set<SQSResource> sqsResourceList = fetcher.fetchResources(region, resources, details);
        for (SQSResource sqsResource : sqsResourceList) {
            String queueName = sqsResource.getResourceName();
            Double totalUsage = (Double) fetcher.getUsage(region, queueName, lastUsage);
            if (totalUsage > 0) {
                if (force) {
                    details.add("SQS queue seems in use, but still deleting with force: [" + queueName + "], totalUsage: [" + totalUsage + "]");
                    LOGGER.warn("SQS queue seems in use, but still deleting with force: [" + queueName + "], totalUsage: [" + totalUsage + "]");
                } else {
                    details.add("SQS queue seems in use, not deleting: [" + queueName + "], totalUsage: [" + totalUsage + "]");
                    LOGGER.warn("SQS queue seems in use, not deleting: [" + queueName + "], totalUsage: [" + totalUsage + "]");
                    continue;
                }
            }
            queuesToDelete.add(sqsResource);
        }

        StringBuilder info = new StringBuilder()
                .append("The resources will be terminated regarding ").append(ticket).append("\n")
                .append("* Dry-Run: ").append(!apply).append("\n")
                .append("* Queues: ").append(queuesToDelete).append("\n");

        info.append("Details:\n");
        details.forEach(d -> info.append("-- ").append(d).append("\n"));
        LOGGER.info(info);

        return queuesToDelete;
    }

    @Override
    protected void apply(Set<SQSResource> resources, boolean apply) {
        List<String> details = new ArrayList<>();
        Set<String> queuesToDelete = new LinkedHashSet<>();
        Set<String> lambdaTriggersToDelete = new LinkedHashSet<>();
        Set<String> snsSubscriptionsToDelete = new LinkedHashSet<>();
        Set<String> cloudwatchAlarmsToDelete = new LinkedHashSet<>();

        resources.forEach(resource -> {
            queuesToDelete.add(resource.getResourceName());
            lambdaTriggersToDelete.addAll(resource.getLambdaTriggers());
            snsSubscriptionsToDelete.addAll(resource.getSnsSubscriptions());
            cloudwatchAlarmsToDelete.addAll(resource.getCloudwatchAlarms());
        });

        details.add("Lambda event-source mappings: " + lambdaTriggersToDelete);
        details.add("SNS subscriptions: " + snsSubscriptionsToDelete);
        details.add("Cloudwatch alarms: " + cloudwatchAlarmsToDelete);

        StringBuilder info = new StringBuilder();
        details.forEach(d -> info.append("-- ").append(d).append("\n"));
        LOGGER.info(info);

        if (!resources.isEmpty() && apply) {
            AmazonSQS sqsClient = AWSClientProvider.getInstance(getConfiguration()).getAmazonSQS();
            AmazonSNS snsClient = AWSClientProvider.getInstance(getConfiguration()).getAmazonSNS();
            AWSLambda lambdaClient = AWSClientProvider.getInstance(getConfiguration()).getAmazonLambda();
            AmazonCloudWatch cloudWatchClient = AWSClientProvider.getInstance(getConfiguration()).getAmazonCloudWatch();

            LOGGER.info("Terminating the resources...");

            lambdaTriggersToDelete.forEach(r -> lambdaClient.deleteEventSourceMapping(new DeleteEventSourceMappingRequest().withUUID(r)));

            snsSubscriptionsToDelete.forEach(snsClient::unsubscribe);

            if (!cloudwatchAlarmsToDelete.isEmpty()) {
                cloudWatchClient.deleteAlarms(new DeleteAlarmsRequest().withAlarmNames(cloudwatchAlarmsToDelete));
            }

            queuesToDelete.forEach(sqsClient::deleteQueue);
        }
    }

    @Override
    protected void afterApply(Set<SQSResource> resources) {
        LOGGER.info("Succeed.");
    }

    void setFetchResourceFactory(ResourceFetcherFactory<SQSResource> resourceFetcherFactory) {
        this.resourceFetcherFactory = resourceFetcherFactory;
    }
}
