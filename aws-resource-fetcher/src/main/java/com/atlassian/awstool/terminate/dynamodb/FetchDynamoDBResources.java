package com.atlassian.awstool.terminate.dynamodb;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.*;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.ListTablesRequest;
import com.amazonaws.services.dynamodbv2.model.ListTablesResult;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.atlassian.awstool.terminate.FetchResources;
import com.atlassian.awstool.terminate.FetchResourcesWithProvider;
import com.atlassian.awstool.terminate.FetcherConfiguration;
import credentials.AwsClientProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author Orhun Dalabasmaz
 * @version 10.03.2021
 */

public class FetchDynamoDBResources extends FetchResourcesWithProvider implements FetchResources<DynamoDBResource> {
    private static final Logger LOGGER = LogManager.getLogger(FetchDynamoDBResources.class);

    public FetchDynamoDBResources(FetcherConfiguration configuration) {
        super(configuration);
    }

    @Override
    public Object getUsage(String region, String resource, int lastDays) {
        AmazonCloudWatch cloudWatchClient = AwsClientProvider.getInstance(getConfiguration()).getAmazonCloudWatch();

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
                                                .withMetricName("ConsumedReadCapacityUnits")
                                                .withDimensions(new Dimension()
                                                        .withName("TableName")
                                                        .withValue(resource)
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
                                                        .withValue(resource)
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
            LOGGER.warn("totalUsage metric is not present for table: " + resource);
        }

        return totalUsage;
    }

    @Override
    public void listResources(String region, Consumer<List<String>> consumer) {
        consume((nextMarker) -> {
            ListTablesResult listTablesResult = AwsClientProvider.getInstance(getConfiguration()).getAmazonDynamoDB().listTables(new ListTablesRequest().withExclusiveStartTableName(nextMarker));
            consumer.accept(listTablesResult.getTableNames());
            return listTablesResult.getLastEvaluatedTableName();
        });
    }

    @Override
    public List<DynamoDBResource> fetchResources(String region, List<String> resources, List<String> details) {
        AmazonDynamoDB dynamoDBClient = AwsClientProvider.getInstance(getConfiguration()).getAmazonDynamoDB();

        AmazonCloudWatch cloudWatchClient = AwsClientProvider.getInstance(getConfiguration()).getAmazonCloudWatch();

        List<DynamoDBResource> dynamoDBResourceList = new ArrayList<>();

        // process each dynamodb tables
        for (String tableName : resources) {
            try {
                LinkedHashSet<String> cloudwatchAlarms = new LinkedHashSet<>();

                // get table
                TableDescription table = dynamoDBClient.describeTable(tableName).getTable();
                Long itemCount = table.getItemCount();

                // Cloudwatch alarms
                cloudWatchClient.describeAlarms(new DescribeAlarmsRequest().withAlarmNamePrefix("DynamoDB table " + tableName + " "))
                        .getMetricAlarms().stream().map(MetricAlarm::getAlarmName)
                        .forEach(cloudwatchAlarms::add);

                DynamoDBResource dynamodbResource = new DynamoDBResource().setResourceName(tableName);
                dynamodbResource.getCloudwatchAlarmList().addAll(cloudwatchAlarms);
                dynamoDBResourceList.add(dynamodbResource);

                details.add(String.format("Resources info for: [%s], [%s] items on table, cw alarms: %s",
                        tableName, itemCount, cloudwatchAlarms));

            } catch (ResourceNotFoundException ex) {
                details.add("!!! DynamoDB table not exists: " + tableName);
                LOGGER.warn("DynamoDB table not exists: " + tableName);
            }
        }
        return dynamoDBResourceList;
    }
}
