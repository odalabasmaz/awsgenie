package io.awsgenie.fetcher.sns;

import com.amazonaws.arn.Arn;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.*;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.ResourceNotFoundException;
import com.amazonaws.services.sns.model.*;
import io.awsgenie.fetcher.ResourceFetcher;
import io.awsgenie.fetcher.ResourceFetcherConfiguration;
import io.awsgenie.fetcher.ResourceFetcherWithProvider;
import io.awsgenie.fetcher.credentials.AWSClientProvider;
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

public class SNSResourceFetcher extends ResourceFetcherWithProvider implements ResourceFetcher<SNSResource> {
    private static final Logger LOGGER = LogManager.getLogger(SNSResourceFetcher.class);

    public SNSResourceFetcher(ResourceFetcherConfiguration configuration) {
        super(configuration);
    }

    @Override
    public Object getUsage(String region, String resource, int lastDays) {
        AmazonCloudWatch cloudWatchClient = AWSClientProvider.getInstance(getConfiguration()).getAmazonCloudWatch();

        Date endDate = new Date();
        Date startDate = new Date(endDate.getTime() - TimeUnit.DAYS.toMillis(lastDays));
        Integer period = ((Long) TimeUnit.DAYS.toSeconds(lastDays)).intValue();

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
    public List<SNSResource> fetchResources( List<String> resources, List<String> details) {
        AmazonSNS snsClient = AWSClientProvider.getInstance(getConfiguration()).getAmazonSNS();
        AmazonCloudWatch cloudWatchClient = AWSClientProvider.getInstance(getConfiguration()).getAmazonCloudWatch();

        List<SNSResource> snsResourceList = new ArrayList<>();
        Map<String, String> topics = new LinkedHashMap<>();
        String nextToken = null;

        do {
            ListTopicsResult listTopicsResult = snsClient.listTopics(new ListTopicsRequest().withNextToken(nextToken));
            listTopicsResult.getTopics()
                    .stream()
                    .map(Topic::getTopicArn)
                    .filter(topicArn -> resources.contains(getResourceFromArn(topicArn)))
                    .forEach(topicArn -> topics.put(getResourceFromArn(topicArn), topicArn));
            nextToken = listTopicsResult.getNextToken();
        } while (nextToken != null);

        for (String topicName : resources) {
            String topicArn = topics.get(topicName);
            LinkedHashSet<String> cloudwatchAlarms = new LinkedHashSet<>();

            try {
                List<String> subscriptions = new LinkedList<>();

                do {
                    ListSubscriptionsByTopicResult listSubscriptionsByTopicResult = snsClient
                            .listSubscriptionsByTopic(new ListSubscriptionsByTopicRequest(topicArn, nextToken));
                    List<String> subscriptionsPart = listSubscriptionsByTopicResult.getSubscriptions()
                            .stream()
                            .map(Subscription::getSubscriptionArn)
                            .collect(Collectors.toList());
                    subscriptions.addAll(subscriptionsPart);
                    nextToken = listSubscriptionsByTopicResult.getNextToken();
                } while (nextToken != null);

                // Cloudwatch alarms
                cloudWatchClient.describeAlarms(new DescribeAlarmsRequest().withAlarmNamePrefix("SNS Notification Failure-" + topicName + "-" + getConfiguration().getRegion()))
                        .getMetricAlarms().stream().map(MetricAlarm::getAlarmName)
                        .forEach(cloudwatchAlarms::add);

                snsResourceList.add(new SNSResource().setResourceName(topicArn).setCloudwatchAlarms(cloudwatchAlarms));

                details.add(String.format("Resources info for: [%s], subscriptions: %s, cw alarms: %s",
                        topicName, subscriptions, cloudwatchAlarms));
            } catch (ResourceNotFoundException ex) {
                details.add("!!! Topic not exists: " + topicName);
                LOGGER.warn("Topic not exists: " + topicName);
            }
        }
        return snsResourceList;
    }

    @Override
    public void listResources( Consumer<List<String>> consumer) {
        consume((String nextMarker) -> {
            List<String> snsResourceNameList = new ArrayList<>();
            ListTopicsResult listTopicsResult = AWSClientProvider.getInstance(getConfiguration()).getAmazonSNS().listTopics(new ListTopicsRequest().withNextToken(nextMarker));
            for (Topic topic : listTopicsResult.getTopics()) {
                snsResourceNameList.add(getResourceFromArn(topic.getTopicArn()));
            }

            consumer.accept(snsResourceNameList);
            return listTopicsResult.getNextToken();
        });
    }

    private String getResourceFromArn(String arn) {
        return Arn.fromString(arn).getResource().getResource();
    }
}
