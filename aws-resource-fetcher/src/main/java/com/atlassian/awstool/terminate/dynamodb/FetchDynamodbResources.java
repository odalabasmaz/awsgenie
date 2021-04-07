package com.atlassian.awstool.terminate.dynamodb;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.*;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.atlassian.awstool.terminate.AWSResource;
import com.atlassian.awstool.terminate.FetchResources;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author Orhun Dalabasmaz
 * @version 10.03.2021
 */

public class FetchDynamodbResources implements FetchResources {
    private static final Logger LOGGER = LogManager.getLogger(FetchDynamodbResources.class);

    private final AWSCredentialsProvider credentialsProvider;

    public FetchDynamodbResources(AWSCredentialsProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
    }

    @Override
    public List<? extends AWSResource> fetchResources(String region, List<String> resources, List<String> details) {
        AmazonDynamoDB dynamoDBClient = AmazonDynamoDBClient
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

        List<DynamodbResource> dynamodbResourceList = new ArrayList<>();

        // process each dynamodb tables
        for (String tableName : resources) {
            try {
                LinkedHashSet<String> cloudwatchAlarms = new LinkedHashSet<>();

                // get table
                TableDescription table = dynamoDBClient.describeTable(tableName).getTable();
                Long itemCount = table.getItemCount();

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
                                                        .withMetricName("ConsumedReadCapacityUnits")
                                                        .withDimensions(new Dimension()
                                                                .withName("TableName")
                                                                .withValue(tableName)
                                                        )
                                                        .withNamespace("AWS/DynamoDB")
                                                )
                                                .withPeriod(period)
                                        ),
                                new MetricDataQuery()
                                        .withId("m2")
                                        .withMetricStat(new MetricStat()
                                                .withStat("Sum")
                                                .withMetric(new Metric()
                                                        .withMetricName("ConsumedWriteCapacityUnits")
                                                        .withDimensions(new Dimension()
                                                                .withName("TableName")
                                                                .withValue(tableName)
                                                        )
                                                        .withNamespace("AWS/DynamoDB")
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
                    LOGGER.warn("totalUsage metric is not present for table: " + tableName);
                }

                // Cloudwatch alarms
                cloudWatchClient.describeAlarms(new DescribeAlarmsRequest().withAlarmNamePrefix("DynamoDB table " + tableName + " "))
                        .getMetricAlarms().stream().map(MetricAlarm::getAlarmName)
                        .forEach(cloudwatchAlarms::add);

                DynamodbResource dynamodbResource = new DynamodbResource().setResourceName(tableName).setTotalUsage(totalUsage);
                dynamodbResource.getCloudwatchAlarmList().addAll(cloudwatchAlarms);
                dynamodbResourceList.add(dynamodbResource);

                details.add(String.format("Resources info for: [%s], [%s] items on table, total usage for last week: [%s], cw alarms: %s",
                        tableName, itemCount, totalUsage, cloudwatchAlarms));

            } catch (ResourceNotFoundException ex) {
                details.add("!!! DynamoDB table not exists: " + tableName);
                LOGGER.warn("DynamoDB table not exists: " + tableName);
            }
        }
        return dynamodbResourceList;
    }

    @Override
    public void listResources(String region, Consumer<List<? extends AWSResource>> consumer) {

    }
}
