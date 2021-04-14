package io.awsgenie.fetcher.sqs;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.*;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.EventSourceMappingConfiguration;
import com.amazonaws.services.lambda.model.ListEventSourceMappingsRequest;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.ListSubscriptionsRequest;
import com.amazonaws.services.sns.model.ListSubscriptionsResult;
import com.amazonaws.services.sns.model.Subscription;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.*;
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

public class SQSResourceFetcher extends ResourceFetcherWithProvider implements ResourceFetcher<SQSResource> {
    private static final Logger LOGGER = LogManager.getLogger(SQSResourceFetcher.class);

    public SQSResourceFetcher(ResourceFetcherConfiguration configuration) {
        super(configuration);
    }

    @Override
    public List<SQSResource> fetchResources(List<String> resources, List<String> details) {
        AmazonSQS sqsClient = AWSClientProvider.getInstance(getConfiguration()).getAmazonSQS();
        AmazonSNS snsClient = AWSClientProvider.getInstance(getConfiguration()).getAmazonSNS();
        AWSLambda lambdaClient = AWSClientProvider.getInstance(getConfiguration()).getAmazonLambda();
        AmazonCloudWatch cloudWatchClient = AWSClientProvider.getInstance(getConfiguration()).getAmazonCloudWatch();

        // Resources to be removed

        // build sns subscription - sqs name mapping
        Map<String, List<String>> sqsSnsMapping = new HashMap<>();
        String nextToken = null;
        do {
            ListSubscriptionsResult result = snsClient.listSubscriptions(
                    new ListSubscriptionsRequest().withNextToken(nextToken));
            nextToken = result.getNextToken();

            for (Subscription subs : result.getSubscriptions()) {
                String protocol = subs.getProtocol();
                String endpoint = subs.getEndpoint();
                if (!protocol.equals("sqs") || endpoint.contains("_cmd_")) {
                    continue;
                }

                //String snsName = subs.getTopicArn().split(":")[5];
                String queueName = endpoint.split(":")[5];
                if (!resources.contains(queueName)) {
                    continue;
                }

                String subscriptionArn = subs.getSubscriptionArn();
                List<String> snsList = sqsSnsMapping.computeIfAbsent(queueName, k -> new ArrayList<>());
                snsList.add(subscriptionArn);

                if (sqsSnsMapping.size() == resources.size()) {
                    // we found all we need
                    nextToken = null;
                    break;
                }
            }
        } while (nextToken != null);

        List<SQSResource> sqsResourceList = new ArrayList<>();
        // process each queue
        for (String queueName : resources) {
            try {
                // QUEUE
                String queueUrl = sqsClient.getQueueUrl(new GetQueueUrlRequest().withQueueName(queueName)).getQueueUrl();
                Map<String, String> attributes = sqsClient.getQueueAttributes(new GetQueueAttributesRequest().withQueueUrl(queueUrl).withAttributeNames("All")).getAttributes();
                String numberOfMessages = attributes.get("ApproximateNumberOfMessages");
                String queueArn = attributes.get("QueueArn");
                //String dlqUrlArn = attributes.get("RedrivePolicy") != null ? JSON.parse(attributes.get("RedrivePolicy")).get("deadLetterTargetArn").toString() : null;

                // Cloudwatch alarms
                List<String> cwAlarms = cloudWatchClient.describeAlarms(new DescribeAlarmsRequest().withAlarmNamePrefix("SQS Queue " + queueName + " "))
                        .getMetricAlarms().stream().map(MetricAlarm::getAlarmName)
                        .collect(Collectors.toList());

                // SNS subscriptions
                List<String> snsSubscriptionArns = sqsSnsMapping.get(queueName);
                if (snsSubscriptionArns == null) snsSubscriptionArns = Collections.emptyList();

                // Lambda triggers
                ListEventSourceMappingsRequest listEventSourceMappingsRequest = new ListEventSourceMappingsRequest();
                listEventSourceMappingsRequest.putCustomQueryParameter("EventSourceArn", queueArn);
                List<EventSourceMappingConfiguration> eventSourceMappings = lambdaClient.listEventSourceMappings(listEventSourceMappingsRequest).getEventSourceMappings();
                List<String> functions = eventSourceMappings.stream().map(EventSourceMappingConfiguration::getFunctionArn).map(arn -> arn.split(":")[6]).collect(Collectors.toList());
                List<String> eventSourceIds = eventSourceMappings.stream().map(EventSourceMappingConfiguration::getUUID).collect(Collectors.toList());

                details.add(String.format("Resources info for: [%s], there are [%s] message(s) in queue, sns subscription(s): %s, lambda trigger(s): %s, cw alarms: %s",
                        queueName, numberOfMessages, snsSubscriptionArns, functions, cwAlarms));
                SQSResource sqsResource = new SQSResource().setResourceName(queueUrl);
                sqsResource.getCloudwatchAlarms().addAll(cwAlarms);
                sqsResource.getSnsSubscriptions().addAll(snsSubscriptionArns);
                sqsResource.getLambdaTriggers().addAll(eventSourceIds);

                sqsResourceList.add(sqsResource);
            } catch (QueueDoesNotExistException ex) {
                details.add("!!! SQS resource not exists: " + queueName);
                LOGGER.warn("SQS resource not exists: " + queueName);
            }
        }
        return sqsResourceList;
    }

    @Override
    public void listResources(Consumer<List<String>> consumer) {
        consume((nextMarker) -> {
            List<String> sqsResourceNameList = new ArrayList<>();
            ListQueuesResult listQueuesResult = AWSClientProvider.getInstance(getConfiguration()).getAmazonSQS().listQueues(new ListQueuesRequest().withNextToken(nextMarker));

            for (String queueUrl : listQueuesResult.getQueueUrls()) {
                sqsResourceNameList.add(getQueueNameFromURL(queueUrl));
            }

            consumer.accept(sqsResourceNameList);
            return listQueuesResult.getNextToken();
        });
    }

    @Override
    public Object getUsage(String region, String resource, int lastDays) {
        AmazonCloudWatch cloudWatchClient = AWSClientProvider.getInstance(getConfiguration()).getAmazonCloudWatch();

        Date endDate = new Date();
        Date startDate = new Date(endDate.getTime() - TimeUnit.DAYS.toMillis(lastDays));
        Integer period = ((Long) TimeUnit.DAYS.toSeconds(lastDays)).intValue();

        // check RW usages for last week
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
                                                .withMetricName("NumberOfMessagesSent")
                                                .withDimensions(new Dimension()
                                                        .withName("QueueName")
                                                        .withValue(resource)
                                                )
                                                .withNamespace("AWS/SQS")
                                        )
                                        .withPeriod(period)
                                ),
                        new MetricDataQuery()
                                .withId("m2")
                                .withMetricStat(new MetricStat()
                                        .withStat("Sum")
                                        .withMetric(new Metric()
                                                .withMetricName("NumberOfMessagesReceived")
                                                .withDimensions(new Dimension()
                                                        .withName("QueueName")
                                                        .withValue(resource)
                                                )
                                                .withNamespace("AWS/SQS")
                                        )
                                        .withPeriod(period)
                                ),
                        new MetricDataQuery()
                                .withId("totalUsage")
                                .withExpression("m1+m2")
                )
        );
        //double totalUsage = result.getMetricDataResults().get(0).getValues().get(0);
        Double totalUsage = 0d;

        Optional<MetricDataResult> optionalMetricDataResult = result.getMetricDataResults().stream().filter(r -> r.getId().equals("totalUsage")).findFirst();
        if (optionalMetricDataResult.isPresent() && optionalMetricDataResult.get().getValues().size() > 0) {
            //Double totalUsage = result.getMetricDataResults().stream().filter(r -> r.getId().equals("totalUsage")).findFirst().map(u -> u.getValues().get(0)).orElse(0.0);
            totalUsage = optionalMetricDataResult.get().getValues().get(0);
        } else {
            LOGGER.warn("totalUsage metric is not present for queue: " + resource);
        }

        return totalUsage;
    }

    private String getQueueNameFromURL(String queueURL) {
        return queueURL.substring(queueURL.lastIndexOf("/") + 1);
    }
}
