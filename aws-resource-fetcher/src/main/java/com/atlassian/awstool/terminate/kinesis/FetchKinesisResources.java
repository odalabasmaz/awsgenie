package com.atlassian.awstool.terminate.kinesis;

import com.amazonaws.arn.Arn;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.*;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClient;
import com.amazonaws.services.lambda.model.EventSourceMappingConfiguration;
import com.amazonaws.services.lambda.model.ListEventSourceMappingsResult;
import com.atlassian.awstool.terminate.AWSResource;
import com.atlassian.awstool.terminate.FetchResources;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author Celal Emre CICEK
 * @version 7.04.2021
 */

public class FetchKinesisResources implements FetchResources {
    private static final Logger LOGGER = LogManager.getLogger(FetchKinesisResources.class);

    private final AWSCredentialsProvider credentialsProvider;

    public FetchKinesisResources(AWSCredentialsProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
    }

    @Override
    public List<? extends AWSResource> fetchResources(String region, String service, List<String> resources, List<String> details) {
        AmazonCloudWatch cloudWatchClient = AmazonCloudWatchClient
                .builder()
                .withRegion(region)
                .withCredentials(credentialsProvider)
                .build();

        AWSLambda lambdaClient = AWSLambdaClient
                .builder()
                .withRegion(region)
                .withCredentials(credentialsProvider)
                .build();

        Date endDate = new Date();
        Date startDate = new Date(endDate.getTime() - TimeUnit.DAYS.toMillis(7));
        Integer period = ((Long) TimeUnit.DAYS.toSeconds(7)).intValue();

        Map<String, List<String>> eventSourceMappings = new LinkedHashMap<>();
        String marker;

        do {
            ListEventSourceMappingsResult listEventSourceMappingsResult = lambdaClient.listEventSourceMappings();
            List<EventSourceMappingConfiguration> mappings = listEventSourceMappingsResult.getEventSourceMappings();
            mappings
                    .stream()
                    .filter(m -> m.getEventSourceArn().startsWith("arn:aws:kinesis"))
                    .forEach(m -> {
                        String streamName = getResourceFromArn(m.getEventSourceArn());
                        String lambdaName = getResourceFromArn(m.getFunctionArn());
                        eventSourceMappings.computeIfAbsent(streamName, k -> new LinkedList<>());
                        eventSourceMappings.get(streamName).add(lambdaName);
                    });
            marker = listEventSourceMappingsResult.getNextMarker();
        } while (marker != null);

        List<KinesisResource> kinesisResourceList = new ArrayList<>();

        for (String stream : resources) {
            try {
                LinkedHashSet<String> cloudwatchAlarms = new LinkedHashSet<>();

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
                                                        .withMetricName("GetRecords.Bytes")
                                                        .withDimensions(new Dimension()
                                                                .withName("StreamName")
                                                                .withValue(stream)
                                                        )
                                                        .withNamespace("AWS/Kinesis")
                                                )
                                                .withPeriod(period)
                                        ),
                                new MetricDataQuery()
                                        .withId("m2")
                                        .withMetricStat(new MetricStat()
                                                .withStat("Sum")
                                                .withMetric(new Metric()
                                                        .withMetricName("IncomingBytes")
                                                        .withDimensions(new Dimension()
                                                                .withName("StreamName")
                                                                .withValue(stream)
                                                        )
                                                        .withNamespace("AWS/Kinesis")
                                                )
                                                .withPeriod(period)
                                        ),
                                new MetricDataQuery()
                                        .withId("totalUsage")
                                        .withExpression("m1+m2")
                        )
                );

                Double totalUsage = 0d;
                Optional<MetricDataResult> optionalMetricDataResult = result.getMetricDataResults()
                        .stream()
                        .filter(r -> r.getId().equals("totalUsage"))
                        .findFirst();

                if (optionalMetricDataResult.isPresent() && optionalMetricDataResult.get().getValues().size() > 0) {
                    totalUsage = optionalMetricDataResult.get().getValues().get(0);
                } else {
                    LOGGER.warn("totalUsage metric is not present for stream: " + stream);
                }

                // Cloudwatch alarms
                cloudWatchClient.describeAlarms(new DescribeAlarmsRequest().withAlarmNamePrefix("Kinesis stream " + stream + " is"))
                        .getMetricAlarms().stream().map(MetricAlarm::getAlarmName)
                        .forEach(cloudwatchAlarms::add);

                KinesisResource kinesisResource = new KinesisResource().setResourceName(stream).setTotalUsage(totalUsage);
                kinesisResource.getCloudwatchAlarmList().addAll(cloudwatchAlarms);
                kinesisResourceList.add(kinesisResource);

                List<String> lambdas = eventSourceMappings.get(stream);

                details.add(String.format("Resources info for: [%s], lambdas this stream triggers: [%s], total usage for last week: [%s], cw alarms: %s",
                        stream, lambdas, totalUsage, cloudwatchAlarms));

            } catch (ResourceNotFoundException ex) {
                details.add("!!! Kinesis stream not exists: " + stream);
                LOGGER.warn("Kinesis strea not exists: " + stream);
            }
        }

        return kinesisResourceList;
    }

    private String getResourceFromArn(String arn) {
        return Arn.fromString(arn).getResource().getResource();
    }
}
