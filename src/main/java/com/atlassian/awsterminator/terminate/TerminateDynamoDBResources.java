package com.atlassian.awsterminator.terminate;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.*;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Orhun Dalabasmaz
 * @version 10.03.2021
 */

public class TerminateDynamoDBResources implements TerminateResources {
    private static final Logger LOGGER = LogManager.getLogger(TerminateDynamoDBResources.class);

    private final AWSCredentialsProvider credentialsProvider;

    public TerminateDynamoDBResources(AWSCredentialsProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
    }

    @Override
    public void terminateResource(String region, String service, List<String> resources, String ticket, boolean apply) {
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

        // Resources to be removed
        LinkedHashSet<String> tablesToDelete = new LinkedHashSet<>();
        LinkedHashSet<String> cloudwatchAlarmsToDelete = new LinkedHashSet<>();

        List<String> details = new LinkedList<>();

        Date endDate = new Date();
        Date startDate = new Date(endDate.getTime() - TimeUnit.DAYS.toMillis(7));
        Integer period = ((Long) TimeUnit.DAYS.toSeconds(7)).intValue();

        // process each dynamodb tables
        for (String tableName : resources) {
            try {
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
                Double totalUsage = result.getMetricDataResults().stream().filter(r -> r.getId().equals("totalUsage")).findFirst().map(u -> u.getValues().get(0)).orElse(0.0);
                if (totalUsage > 0) {
                    details.add("DynamoDB table seems in use, not deleting: [" + tableName + "], totalUsage: [" + totalUsage + "]");
                    LOGGER.warn("DynamoDB table seems in use, not deleting: [" + tableName + "], totalUsage: [" + totalUsage + "]");
                    continue;
                }

                // Cloudwatch alarms
                cloudWatchClient.describeAlarms(new DescribeAlarmsRequest().withAlarmNamePrefix("DynamoDB table " + tableName + " "))
                        .getMetricAlarms().stream().map(MetricAlarm::getAlarmName)
                        .forEach(cloudwatchAlarmsToDelete::add);

                // Add to delete list at last step if gathering the subscriptions fail
                tablesToDelete.add(tableName);

                details.add(String.format("Resources info for: [%s], [%s] items on table, total usage for last week: [%s], cw alarms: %s",
                        tableName, itemCount, totalUsage, cloudwatchAlarmsToDelete));

            } catch (ResourceNotFoundException ex) {
                details.add("!!! DynamoDB table not exists: " + tableName);
                LOGGER.warn("DynamoDB table not exists: " + tableName);
            } /*catch (Exception ex) {
                details.add("!!! Error occurred for: " + tableName);
                LOGGER.error("Error occurred while processing dynamodb table: " + tableName, ex);
            }*/
        }

        StringBuilder info = new StringBuilder()
                .append("The resources will be terminated regarding ").append(ticket).append("\n")
                .append("* Dry-Run: ").append(!apply).append("\n")
                .append("* DynamoDB tables: ").append(tablesToDelete).append("\n")
                .append("* Cloudwatch alarms: ").append(cloudwatchAlarmsToDelete).append("\n");

        info.append("Details:\n");
        details.forEach(d -> info.append("-- ").append(d).append("\n"));
        LOGGER.info(info);

        if (apply) {
            LOGGER.info("Terminating the resources...");

            tablesToDelete.forEach(dynamoDBClient::deleteTable);

            if (!cloudwatchAlarmsToDelete.isEmpty()) {
                cloudWatchClient.deleteAlarms(new DeleteAlarmsRequest().withAlarmNames(cloudwatchAlarmsToDelete));
            }
        }

        LOGGER.info("Succeed.");
    }
}
