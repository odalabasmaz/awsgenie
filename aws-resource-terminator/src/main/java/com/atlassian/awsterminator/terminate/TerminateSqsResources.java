package com.atlassian.awsterminator.terminate;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.DeleteAlarmsRequest;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.DeleteEventSourceMappingRequest;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sqs.AmazonSQS;
import com.atlassian.awsterminator.configuration.Configuration;
import com.atlassian.awsterminator.interceptor.InterceptorRegistry;
import com.atlassian.awstool.terminate.FetchResourceFactory;
import com.atlassian.awstool.terminate.FetchResources;
import com.atlassian.awstool.terminate.FetcherConfiguration;
import com.atlassian.awstool.terminate.Service;
import com.atlassian.awstool.terminate.sqs.SQSResource;
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

public class TerminateSqsResources extends TerminateResourcesWithProvider implements TerminateResources<SQSResource> {
    private static final Logger LOGGER = LogManager.getLogger(TerminateSqsResources.class);

    private FetchResourceFactory<SQSResource> fetchResourceFactory;

    public TerminateSqsResources(AwsClientConfiguration configuration) {
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
        AmazonSQS sqsClient = AwsClientProvider.getInstance(getConfiguration()).getAmazonSQS();
        AmazonSNS snsClient = AwsClientProvider.getInstance(getConfiguration()).getAmazonSNS();
        AWSLambda lambdaClient = AwsClientProvider.getInstance(getConfiguration()).getAmazonLambda();
        AmazonCloudWatch cloudWatchClient = AwsClientProvider.getInstance(getConfiguration()).getAmazonCloudWatch();

        // Resources to be removed
        LinkedHashSet<String> queuesToDelete = new LinkedHashSet<>();
        LinkedHashSet<String> lambdaTriggersToDelete = new LinkedHashSet<>();
        LinkedHashSet<String> snsSubscriptionsToDelete = new LinkedHashSet<>();
        LinkedHashSet<String> cloudwatchAlarmsToDelete = new LinkedHashSet<>();

        List<String> details = new LinkedList<>();

        FetchResources<SQSResource> fetcher = getFetchResourceFactory().getFetcher(service, new FetcherConfiguration(getConfiguration()));
        List<SQSResource> sqsResourceList = fetcher.fetchResources(region, resources, details);
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
            queuesToDelete.add(queueName);
            lambdaTriggersToDelete.addAll(sqsResource.getLambdaTriggers());
            snsSubscriptionsToDelete.addAll(sqsResource.getSnsSubscriptions());
            cloudwatchAlarmsToDelete.addAll(sqsResource.getCloudwatchAlarms());
        }

        StringBuilder info = new StringBuilder()
                .append("The resources will be terminated regarding ").append(ticket).append("\n")
                .append("* Dry-Run: ").append(!apply).append("\n")
                .append("* Lambda event-source mappings: ").append(lambdaTriggersToDelete).append("\n")
                .append("* SNS subscriptions: ").append(snsSubscriptionsToDelete).append("\n")
                .append("* Queues: ").append(queuesToDelete).append("\n")
                .append("* Cloudwatch alarms: ").append(cloudwatchAlarmsToDelete).append("\n");

        info.append("Details:\n");
        details.forEach(d -> info.append("-- ").append(d).append("\n"));
        LOGGER.info(info);

        InterceptorRegistry.getBeforeTerminateInterceptors()
                .forEach(interceptor -> interceptor.intercept(service, sqsResourceList, info.toString(), apply));

        if (apply) {
            LOGGER.info("Terminating the resources...");

            lambdaTriggersToDelete.forEach(r -> lambdaClient.deleteEventSourceMapping(new DeleteEventSourceMappingRequest().withUUID(r)));

            snsSubscriptionsToDelete.forEach(snsClient::unsubscribe);

            if (!cloudwatchAlarmsToDelete.isEmpty()) {
                cloudWatchClient.deleteAlarms(new DeleteAlarmsRequest().withAlarmNames(cloudwatchAlarmsToDelete));
            }

            queuesToDelete.forEach(sqsClient::deleteQueue);
        }

        InterceptorRegistry.getAfterTerminateInterceptors()
                .forEach(interceptor -> interceptor.intercept(service, sqsResourceList, info.toString(), apply));

        LOGGER.info("Succeed.");
    }


    void setFetchResourceFactory(FetchResourceFactory<SQSResource> fetchResourceFactory) {
        this.fetchResourceFactory = fetchResourceFactory;
    }

    //TODO: send to abstract class for all...
    private FetchResourceFactory<SQSResource> getFetchResourceFactory() {
        if (this.fetchResourceFactory != null) {
            return this.fetchResourceFactory;
        } else {
            return new FetchResourceFactory<>();
        }
    }
}
