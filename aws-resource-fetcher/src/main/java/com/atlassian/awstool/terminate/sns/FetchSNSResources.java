package com.atlassian.awstool.terminate.sns;

import com.amazonaws.arn.Arn;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.ResourceNotFoundException;
import com.amazonaws.services.cloudwatch.model.*;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.*;
import com.atlassian.awstool.terminate.AWSResource;
import com.atlassian.awstool.terminate.FetchResources;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author Orhun Dalabasmaz
 * @version 10.03.2021
 */

public class FetchSNSResources implements FetchResources {
    private static final Logger LOGGER = LogManager.getLogger(FetchSNSResources.class);

    private final AWSCredentialsProvider credentialsProvider;
    private final Map<String, AmazonSNS> snsClientMap;

    public FetchSNSResources(AWSCredentialsProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
        this.snsClientMap = new HashMap<>();
    }

    @Override
    public List<? extends AWSResource> fetchResources(String region, List<String> resources, List<String> details) {
        AmazonSNS snsClient = AmazonSNSClient
                .builder()
                .withRegion(region)
                .withCredentials(credentialsProvider)
                .build();

        AmazonCloudWatch cloudWatchClient = AmazonCloudWatchClient
                .builder()
                .withRegion(region)
                .withCredentials(credentialsProvider)
                .build();

        Date endDate = new Date();
        Date startDate = new Date(endDate.getTime() - TimeUnit.DAYS.toMillis(7));
        Integer period = ((Long) TimeUnit.DAYS.toSeconds(7)).intValue();

        List<SNSResource> snsResourceList = new ArrayList<>();
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
            LinkedHashSet<String> cloudwatchAlarms = new LinkedHashSet<>();

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
                        .forEach(cloudwatchAlarms::add);

                snsResourceList.add(new SNSResource().setResourceName(topic.getTopicArn()).setCloudwatchAlarms(cloudwatchAlarms).setPublishCountInLastWeek(totalUsage));

                details.add(String.format("Resources info for: [%s], subscriptions: [%s], total usage for last week: [%s], cw alarms: %s",
                        topicName, subscriptions, totalUsage, cloudwatchAlarms));
            } catch (ResourceNotFoundException ex) {
                details.add("!!! Topic not exists: " + topicName);
                LOGGER.warn("Topic not exists: " + topicName);
            }
        }
        return snsResourceList;
    }

    private String getResourceFromArn(String arn) {
        return Arn.fromString(arn).getResource().getResource();
    }

    @Override
    public void listResources(String region, Consumer<List<String>> consumer) {

        List<String> snsResourceNameList = new ArrayList<>();

        consume((String nextMarker) -> {
            ListTopicsResult listTopicsResult = getSNSClient(region).listTopics(new ListTopicsRequest().withNextToken(nextMarker));
            for (Topic topic : listTopicsResult.getTopics()) {
                snsResourceNameList.add(getResourceFromArn(topic.getTopicArn()));
            }

            consumer.accept(snsResourceNameList);
            return listTopicsResult.getNextToken();
        });
    }

    private AmazonSNS getSNSClient(String region) {
        if (snsClientMap.get(region) == null) {
            snsClientMap.put(region, AmazonSNSClient
                    .builder()
                    .withRegion(region)
                    .withCredentials(credentialsProvider)
                    .build());
        }
        return snsClientMap.get(region);
    }
}
