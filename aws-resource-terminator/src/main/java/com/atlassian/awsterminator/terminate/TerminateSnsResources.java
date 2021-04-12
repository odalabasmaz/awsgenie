package com.atlassian.awsterminator.terminate;

import com.amazonaws.arn.Arn;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.DeleteAlarmsRequest;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.atlassian.awsterminator.configuration.Configuration;
import com.atlassian.awsterminator.interceptor.InterceptorRegistry;
import com.atlassian.awstool.terminate.FetchResourceFactory;
import com.atlassian.awstool.terminate.FetchResources;
import com.atlassian.awstool.terminate.FetcherConfiguration;
import com.atlassian.awstool.terminate.Service;
import com.atlassian.awstool.terminate.sns.SNSResource;
import credentials.AwsClientConfiguration;
import credentials.AwsClientProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * @author Celal Emre CICEK
 * @version 6.04.2021
 */

public class TerminateSnsResources extends TerminateResourcesWithProvider implements TerminateResources<SNSResource> {
    private static final Logger LOGGER = LogManager.getLogger(TerminateSnsResources.class);

    private FetchResourceFactory<SNSResource> fetchResourceFactory;

    public TerminateSnsResources(AwsClientConfiguration configuration) {
        super(configuration);
    }

    @Override
    public void terminateResource(String region, Service service, List<String> resources, String ticket, boolean apply) throws Exception {
        AmazonSNS snsClient = AwsClientProvider.getInstance(getConfiguration()).getAmazonSNS();

        AmazonCloudWatch cloudWatchClient = AwsClientProvider.getInstance(getConfiguration()).getAmazonCloudWatch();

        HashSet<String> topicsToDelete = new LinkedHashSet<>();
        HashSet<String> cloudwatchAlarmsToDelete = new LinkedHashSet<>();

        List<String> details = new LinkedList<>();

        /*Date endDate = new Date();
        Date startDate = new Date(endDate.getTime() - TimeUnit.DAYS.toMillis(7));
        Integer period = ((Long) TimeUnit.DAYS.toSeconds(7)).intValue();

        List<Topic> topics = new LinkedList<>();
        String nextToken = null;

        do {
            ListTopicsResult listTopicsResult = snsClient.listTopics(new ListTopicsRequest().withNextToken(nextToken));
            topics.addAll(listTopicsResult.getTopics()
                    .stream()
                    .filter(topic -> resources.contains(getResourceFromArn(topic.getTopicArn())))
                    .collect(Collectors.toList()));
            nextToken = listTopicsResult.getNextToken();
        } while (nextToken != null);

        for (Topic topic : topics) {
            String topicName = getResourceFromArn(topic.getTopicArn());

            try {
                // check usages for last week
                GetMetricDataResult result = cloudWatchClient.getMetricData(new GetMetricDataRequest()
                        .withStartTime(startDate)
                        .withEndTime(endDate)
                        .withMaxDatapoints(100)
                        .withMetricDataQueries(
                                new MetricDataQuery()
                                        .withId("m1")
                                        .withMetricStat(new MetricStat()
                                                .withStat("Sum")
                                                .withMetric(new Metric()
                                                        .withMetricName("NumberOfMessagesPublished")
                                                        .withDimensions(new Dimension()
                                                                .withName("TopicName")
                                                                .withValue(topicName)
                                                        )
                                                        .withNamespace("AWS/SNS")
                                                )
                                                .withPeriod(period)
                                        )
                        )
                );

                double totalUsage = 0;

                if (result.getMetricDataResults().get(0).getValues().size() > 0) {
                    totalUsage = result.getMetricDataResults().get(0).getValues().get(0);
                } else {
                    details.add("No metric found for topic: [" + topicName + "]");
                    LOGGER.warn("No metric found for topic: [" + topicName + "]");
                }

                if (totalUsage > 0) {
                    details.add("Topic seems in use, not deleting: [" + topicName + "], totalUsage: [" + totalUsage + "]");
                    LOGGER.warn("Topic seems in use, not deleting: [" + topicName + "], totalUsage: [" + totalUsage + "]");
                    continue;
                }

                List<String> subscriptions = new LinkedList<>();

                do {
                    ListSubscriptionsByTopicResult listSubscriptionsByTopicResult = snsClient
                            .listSubscriptionsByTopic(new ListSubscriptionsByTopicRequest(topic.getTopicArn(), nextToken));
                    List<String> subscriptionsPart = listSubscriptionsByTopicResult.getSubscriptions()
                            .stream()
                            .map(Subscription::getSubscriptionArn)
                            .collect(Collectors.toList());
                    subscriptions.addAll(subscriptionsPart);
                    nextToken = listSubscriptionsByTopicResult.getNextToken();
                } while (nextToken != null);

                // Cloudwatch alarms
                cloudWatchClient.describeAlarms(new DescribeAlarmsRequest().withAlarmNamePrefix("SNS Notification Failure-" + topicName + "-" + region))
                        .getMetricAlarms().stream().map(MetricAlarm::getAlarmName)
                        .forEach(cloudwatchAlarmsToDelete::add);

                // Add to delete list at last step if gathering the subscriptions fail
                topicsToDelete.add(topic.getTopicArn());

                details.add(String.format("Resources info for: [%s], subscriptions: [%s], total usage for last week: [%s], cw alarms: %s",
                        topicName, subscriptions, totalUsage, cloudwatchAlarmsToDelete));
            } catch (ResourceNotFoundException ex) {
                details.add("!!! Topic not exists: " + topicName);
                LOGGER.warn("Topic not exists: " + topicName);
            }
        }*/

        FetchResources fetcher = getFetchResourceFactory().getFetcher(Service.SNS, new FetcherConfiguration(getConfiguration()));
        List<SNSResource> snsResourceList = (List<SNSResource>) fetcher.fetchResources(region, resources, details);

        for (SNSResource snsResource : snsResourceList) {
            Double publishCountInLastWeek = (Double) fetcher.getUsage(region, snsResource.getResourceName());
            if (publishCountInLastWeek > 0) {
                details.add("Topic seems in use, not deleting: [" + snsResource.getResourceName() + "], totalUsage: [" + publishCountInLastWeek + "]");
                LOGGER.warn("Topic seems in use, not deleting: [" + snsResource.getResourceName() + "], totalUsage: [" + publishCountInLastWeek + "]");
                continue;
            }
            topicsToDelete.add(snsResource.getResourceName());
            cloudwatchAlarmsToDelete.addAll(snsResource.getCloudwatchAlarms());
        }

        StringBuilder info = new StringBuilder()
                .append("The resources will be terminated regarding ").append(ticket).append("\n")
                .append("* Dry-Run: ").append(!apply).append("\n")
                .append("* Topics: ").append(topicsToDelete).append("\n")
                .append("* Cloudwatch alarms: ").append(cloudwatchAlarmsToDelete).append("\n");

        info.append("Details:\n");
        details.forEach(d -> info.append("-- ").append(d).append("\n"));
        LOGGER.info(info);

        InterceptorRegistry.getBeforeTerminateInterceptors()
                .forEach(interceptor -> interceptor.intercept(service, snsResourceList, info.toString(), apply));

        if (apply) {
            LOGGER.info("Terminating the resources...");

            topicsToDelete.forEach(snsClient::deleteTopic);

            if (!cloudwatchAlarmsToDelete.isEmpty()) {
                cloudWatchClient.deleteAlarms(new DeleteAlarmsRequest().withAlarmNames(cloudwatchAlarmsToDelete));
            }
        }

        InterceptorRegistry.getAfterTerminateInterceptors()
                .forEach(interceptor -> interceptor.intercept(service, snsResourceList, info.toString(), apply));

        LOGGER.info("Succeed.");
    }

    private String getResourceFromArn(String arn) {
        return Arn.fromString(arn).getResource().getResource();
    }


    void setFetchResourceFactory(FetchResourceFactory<SNSResource> fetchResourceFactory) {
        this.fetchResourceFactory = fetchResourceFactory;
    }

    private FetchResourceFactory<SNSResource> getFetchResourceFactory() {
        if (this.fetchResourceFactory != null) {
            return this.fetchResourceFactory;
        } else {
            return new FetchResourceFactory<>();
        }
    }
}
