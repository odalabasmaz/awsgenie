package io.awsgenie.fetcher.dynamodb;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.*;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.*;
import io.awsgenie.fetcher.ResourceFetcherConfiguration;
import io.awsgenie.fetcher.credentials.AWSClientProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;


/**
 * @author Celal Emre CICEK
 * @version 12.04.2021
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        AWSClientProvider.class
})
@PowerMockIgnore({
        "javax.management.*", "javax.script.*"
})
public class DynamoDBResourceFetcherTest {
    private static final String TEST_REGION = "us-west-2";

    @Mock
    private ResourceFetcherConfiguration resourceFetcherConfiguration;

    @Mock
    private AmazonCloudWatch cloudWatchClient;

    @Mock
    private AmazonDynamoDB dynamoDBClient;

    @Mock
    private AWSClientProvider awsClientProvider;

    private DynamoDBResourceFetcher dynamodbResourceFetcher;

    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(AWSClientProvider.class);
        when(AWSClientProvider.getInstance(resourceFetcherConfiguration))
                .thenReturn(awsClientProvider);
        when(awsClientProvider.getAmazonCloudWatch())
                .thenReturn(cloudWatchClient);
        when(awsClientProvider.getAmazonDynamoDB())
                .thenReturn(dynamoDBClient);

        this.dynamodbResourceFetcher = new DynamoDBResourceFetcher(resourceFetcherConfiguration);
    }

    @Test
    public void listResources() {
        when(dynamoDBClient.listTables(new ListTablesRequest()))
                .thenReturn(new ListTablesResult().withTableNames("table1", "table2").withLastEvaluatedTableName("table2"));
        when(dynamoDBClient.listTables(new ListTablesRequest("table2")))
                .thenReturn(new ListTablesResult().withTableNames("table3", "table4"));


        List<String> actualTables = new ArrayList<>();
        dynamodbResourceFetcher.listResources(TEST_REGION, actualTables::addAll);

        verify(dynamoDBClient, times(2)).listTables(org.mockito.Mockito.any(ListTablesRequest.class));
        assertThat(actualTables.size(), is(equalTo(4)));
        assertThat(actualTables, hasItem("table1"));
        assertThat(actualTables, hasItem("table2"));
        assertThat(actualTables, hasItem("table3"));
        assertThat(actualTables, hasItem("table4"));
    }

    @Test
    public void fetchResources() {
        when(dynamoDBClient.describeTable("table1"))
                .thenReturn(new DescribeTableResult().withTable(new TableDescription().withItemCount(10L)));
        when(dynamoDBClient.describeTable("table2"))
                .thenThrow(new ResourceNotFoundException("table2 not found"));

        when(cloudWatchClient.describeAlarms(new DescribeAlarmsRequest().withAlarmNamePrefix("DynamoDB table table1 ")))
                .thenReturn(new DescribeAlarmsResult().withMetricAlarms(new MetricAlarm().withAlarmName("alarm1")));

        List<String> details = new ArrayList<>();
        ArrayList<String> tables = new ArrayList<>();
        tables.add("table1");
        tables.add("table2");

        List<DynamoDBResource> actualDynamoDBResources = dynamodbResourceFetcher.fetchResources(TEST_REGION, tables, details);

        verify(dynamoDBClient).describeTable("table1");
        verify(dynamoDBClient).describeTable("table2");
        verify(cloudWatchClient).describeAlarms(new DescribeAlarmsRequest().withAlarmNamePrefix("DynamoDB table table1 "));

        assertThat(actualDynamoDBResources.size(), is(equalTo(1)));
        assertThat(actualDynamoDBResources, hasItem(new DynamoDBResource()
                .setResourceName("table1")
                .setCloudwatchAlarmList(new LinkedHashSet<String>() {{
                    add("alarm1");
                }})));
        assertThat(details.size(), is(equalTo(2)));
        assertThat(details, hasItem("Resources info for: [table1], [10] items on table, cw alarms: [alarm1]"));
        assertThat(details, hasItem("!!! DynamoDB table not exists: table2"));
    }

    @Test
    public void getUsage() {
        when(cloudWatchClient.getMetricData(org.mockito.Mockito.any(GetMetricDataRequest.class)))
                .thenReturn(new GetMetricDataResult()
                        .withMetricDataResults(new MetricDataResult().withId("totalUsage").withValues(4.13)));
        Object usage = dynamodbResourceFetcher.getUsage(TEST_REGION, "table1", 7);
        assertThat(usage, is(equalTo(4.13)));

        ArgumentCaptor<GetMetricDataRequest> captor = ArgumentCaptor.forClass(GetMetricDataRequest.class);
        verify(cloudWatchClient).getMetricData(captor.capture());
        GetMetricDataRequest actualRequest = captor.getValue();
        Date endTime = actualRequest.getEndTime();
        Date startTime = actualRequest.getStartTime();
        Integer period = ((Long) TimeUnit.DAYS.toSeconds(7)).intValue();

        assertThat(startTime, is(equalTo(new Date(endTime.getTime() - TimeUnit.DAYS.toMillis(7)))));
        assertThat(actualRequest.getMaxDatapoints(), is(equalTo(100)));
        assertThat(actualRequest.getMetricDataQueries().size(), is(equalTo(3)));
        assertThat(actualRequest.getMetricDataQueries(), hasItem(new MetricDataQuery()
                .withId("m1")
                .withMetricStat(new MetricStat()
                        .withStat("Sum")
                        .withMetric(new Metric()
                                .withMetricName("ConsumedReadCapacityUnits")
                                .withDimensions(new Dimension()
                                        .withName("TableName")
                                        .withValue("table1")
                                )
                                .withNamespace("AWS/DynamoDB")
                        )
                        .withPeriod(period)
                )));
        assertThat(actualRequest.getMetricDataQueries(), hasItem(new MetricDataQuery()
                .withId("m2")
                .withMetricStat(new MetricStat()
                        .withStat("Sum")
                        .withMetric(new Metric()
                                .withMetricName("ConsumedWriteCapacityUnits")
                                .withDimensions(new Dimension()
                                        .withName("TableName")
                                        .withValue("table1")
                                )
                                .withNamespace("AWS/DynamoDB")
                        )
                        .withPeriod(period)
                )));
        assertThat(actualRequest.getMetricDataQueries(), hasItem(new MetricDataQuery()
                .withId("totalUsage")
                .withExpression("m1+m2")));
    }
}