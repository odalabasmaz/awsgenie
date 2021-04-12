package com.atlassian.awstool.terminate.sns;

import com.amazonaws.arn.Arn;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.ResourceNotFoundException;
import com.amazonaws.services.cloudwatch.model.*;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.*;
import com.atlassian.awstool.terminate.FetchResources;
import com.atlassian.awstool.terminate.FetchResourcesWithProvider;
import com.atlassian.awstool.terminate.FetcherConfiguration;
import credentials.AwsClientProvider;
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

public class FetchSNSResources extends FetchResourcesWithProvider implements FetchResources<SNSResource>{
    private static final Logger LOGGER = LogManager.getLogger(FetchSNSResources.class);

    public FetchSNSResources(FetcherConfiguration configuration) {
        super(configuration);
    }

    @Override
    public Object getUsage(String region, String resource) {
        AmazonCloudWatch cloudWatchClient = AwsClientProvider.getInstance(getConfiguration()).getAmazonCloudWatch();

        Date endDate = new Date();
        Date startDate = new Date(endDate.getTime() - TimeUnit.DAYS.toMillis(7));
        Integer period = ((Long) TimeUnit.DAYS.toSeconds(7)).intValue();

        String topicName = getResourceFromArn(resource);

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
            LOGGER.warn("No metric found for topic: [" + topicName + "]");
        }

        return totalUsage;
    }

    @Override
    public List<SNSResource> fetchResources(String region, List<String> resources, List<String> details) {
        AmazonSNS snsClient = AwsClientProvider.getInstance(getConfiguration()).getAmazonSNS();
        AmazonCloudWatch cloudWatchClient =AwsClientProvider.getInstance(getConfiguration()).getAmazonCloudWatch();

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

                snsResourceList.add(new SNSResource().setResourceName(topic.getTopicArn()).setCloudwatchAlarms(cloudwatchAlarms));

                details.add(String.format("Resources info for: [%s], subscriptions: [%s], cw alarms: %s",
                        topicName, subscriptions, cloudwatchAlarms));
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
            ListTopicsResult listTopicsResult = AwsClientProvider.getInstance(getConfiguration()).getAmazonSNS().listTopics(new ListTopicsRequest().withNextToken(nextMarker));
            for (Topic topic : listTopicsResult.getTopics()) {
                snsResourceNameList.add(getResourceFromArn(topic.getTopicArn()));
            }

            consumer.accept(snsResourceNameList);
            return listTopicsResult.getNextToken();
        });
    }
}
