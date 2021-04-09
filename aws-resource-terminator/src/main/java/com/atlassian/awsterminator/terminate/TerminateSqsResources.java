package com.atlassian.awsterminator.terminate;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.DeleteAlarmsRequest;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClient;
import com.amazonaws.services.lambda.model.DeleteEventSourceMappingRequest;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.atlassian.awsterminator.interceptor.InterceptorRegistry;
import com.atlassian.awstool.terminate.FetchResourceFactory;
import com.atlassian.awstool.terminate.FetchResources;
import com.atlassian.awstool.terminate.sqs.SQSResource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Orhun Dalabasmaz
 * @version 10.03.2021
 */

public class TerminateSqsResources implements TerminateResources {
    private static final Logger LOGGER = LogManager.getLogger(TerminateSqsResources.class);

    private final AWSCredentialsProvider credentialsProvider;
    private AmazonSQS sqsClient;
    private AmazonCloudWatch cloudWatchClient;
    private AmazonSNS snsClient;
    private AWSLambda lambdaClient;
    private FetchResourceFactory fetchResourceFactory;

    public TerminateSqsResources(AWSCredentialsProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
    }

    @Override
    public void terminateResource(String region, String service, List<String> resources, String ticket, boolean apply) throws Exception {
        AmazonSQS sqsClient = getSqsClient(region);
        AmazonSNS snsClient = getSnsClient(region);
        AWSLambda lambdaClient = getLambdaClient(region);
        AmazonCloudWatch cloudWatchClient = getCloudWatchClient(region);

        // Resources to be removed
        LinkedHashSet<String> queuesToDelete = new LinkedHashSet<>();
        LinkedHashSet<String> lambdaTriggersToDelete = new LinkedHashSet<>();
        LinkedHashSet<String> snsSubscriptionsToDelete = new LinkedHashSet<>();
        LinkedHashSet<String> cloudwatchAlarmsToDelete = new LinkedHashSet<>();

        List<String> details = new LinkedList<>();

        FetchResources fetcher = getFetchResourceFactory().getFetcher("sqs", credentialsProvider);
        List<SQSResource> sqsResourceList = (List<SQSResource>) fetcher.fetchResources(region, resources, details);
        for (SQSResource sqsResource : sqsResourceList) {
            queuesToDelete.add(sqsResource.getResourceName());
            lambdaTriggersToDelete.addAll(sqsResource.getLambdaTriggers());
            snsSubscriptionsToDelete.addAll(sqsResource.getSnsSubscriptions());
            cloudwatchAlarmsToDelete.addAll(sqsResource.getCloudwatchAlarms());
        }

        StringBuilder info = new StringBuilder()
                .append("The resources will be terminated regarding ").append(ticket).append("\n")
                .append("* Dry-Run: ").append(!apply).append("\n")
                .append("* Lambda event-source mappings: ").append(lambdaTriggersToDelete).append("\n")
                .append("* SNS subscriptions: ").append(snsSubscriptionsToDelete).append("\n")
                //.append("* DLQs: ").append(dlqsToDelete).append("\n")
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

    void setCloudWatchClient(AmazonCloudWatch cloudWatchClient) {
        this.cloudWatchClient = cloudWatchClient;
    }

    private AmazonCloudWatch getCloudWatchClient(String region) {
        if (this.cloudWatchClient != null) {
            return this.cloudWatchClient;
        } else {
            return AmazonCloudWatchClient
                    .builder()
                    .withRegion(region)
                    .withCredentials(credentialsProvider)
                    .build();
        }
    }

    void setSnsClient(AmazonSNS snsClient) {
        this.snsClient = snsClient;
    }

    private AmazonSNS getSnsClient(String region) {
        if (this.snsClient != null) {
            return this.snsClient;
        } else {
            return AmazonSNSClient
                    .builder()
                    .withRegion(region)
                    .withCredentials(credentialsProvider)
                    .build();
        }
    }

    void setLambdaClient(AWSLambda lambdaClient) {
        this.lambdaClient = lambdaClient;
    }

    private AWSLambda getLambdaClient(String region) {
        if (this.lambdaClient != null) {
            return this.lambdaClient;
        } else {
            return AWSLambdaClient
                    .builder()
                    .withRegion(region)
                    .withCredentials(credentialsProvider)
                    .build();
        }
    }

    void setSqsClient(AmazonSQS sqsClient) {
        this.sqsClient = sqsClient;
    }

    private AmazonSQS getSqsClient(String region) {
        if (this.sqsClient != null) {
            return this.sqsClient;
        } else {
            return AmazonSQSClient
                    .builder()
                    .withRegion(region)
                    .withCredentials(credentialsProvider)
                    .build();
        }
    }

    void setFetchResourceFactory(FetchResourceFactory fetchResourceFactory) {
        this.fetchResourceFactory = fetchResourceFactory;
    }

    private FetchResourceFactory getFetchResourceFactory() {
        if (this.fetchResourceFactory != null) {
            return this.fetchResourceFactory;
        } else {
            return new FetchResourceFactory();
        }
    }
}
