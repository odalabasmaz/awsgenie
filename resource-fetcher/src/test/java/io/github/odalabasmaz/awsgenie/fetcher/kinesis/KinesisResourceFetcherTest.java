package io.github.odalabasmaz.awsgenie.fetcher.kinesis;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.*;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.model.ResourceNotFoundException;
import com.amazonaws.services.kinesis.model.*;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.EventSourceMappingConfiguration;
import com.amazonaws.services.lambda.model.ListEventSourceMappingsResult;
import io.github.odalabasmaz.awsgenie.fetcher.ResourceFetcherConfiguration;
import io.github.odalabasmaz.awsgenie.fetcher.credentials.AWSClientProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.*;
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
public class KinesisResourceFetcherTest {
    private static final String TEST_REGION = "us-west-2";
    private static final String TEST_ACCOUNT_ID = "111111111111";

    @Mock
    private ResourceFetcherConfiguration resourceFetcherConfiguration;

    @Mock
    private AmazonCloudWatch cloudWatchClient;

    @Mock
    private AmazonKinesis kinesisClient;

    @Mock
    private AWSLambda lambdaClient;

    @Mock
    private AWSClientProvider awsClientProvider;

    private KinesisResourceFetcher kinesisResourceFetcher;

    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(AWSClientProvider.class);
        when(AWSClientProvider.getInstance(resourceFetcherConfiguration))
                .thenReturn(awsClientProvider);
        when(awsClientProvider.getAmazonCloudWatch())
                .thenReturn(cloudWatchClient);
        when(awsClientProvider.getAmazonKinesis())
                .thenReturn(kinesisClient);
        when(awsClientProvider.getAmazonLambda())
                .thenReturn(lambdaClient);

        this.kinesisResourceFetcher = new KinesisResourceFetcher(resourceFetcherConfiguration);
    }

    @Test
    public void listResources() {
        List<String> actualStreams = new ArrayList<>();

        when(kinesisClient.listStreams(new ListStreamsRequest()))
                .thenReturn(new ListStreamsResult()
                        .withStreamNames("stream1", "stream2")
                        .withHasMoreStreams(true));
        when(kinesisClient.listStreams(new ListStreamsRequest().withExclusiveStartStreamName("stream2")))
                .thenReturn(new ListStreamsResult()
                        .withStreamNames("stream3", "stream4")
                        .withHasMoreStreams(false));

        kinesisResourceFetcher.listResources(TEST_REGION, actualStreams::addAll);

        verify(kinesisClient, times(2)).listStreams(org.mockito.Mockito.any(ListStreamsRequest.class));
        assertThat(actualStreams.size(), is(equalTo(4)));
        assertThat(actualStreams, hasItem("stream1"));
        assertThat(actualStreams, hasItem("stream2"));
        assertThat(actualStreams, hasItem("stream3"));
        assertThat(actualStreams, hasItem("stream4"));
    }

    @Test
    public void fetchResources() {
        when(lambdaClient.listEventSourceMappings())
                .thenReturn(new ListEventSourceMappingsResult()
                        .withEventSourceMappings(
                                new EventSourceMappingConfiguration()
                                        .withEventSourceArn("arn:aws:kinesis:" + TEST_REGION + ":" + TEST_ACCOUNT_ID + ":stream/stream1")
                                        .withFunctionArn("arn:aws:lambda:" + TEST_REGION + ":" + TEST_ACCOUNT_ID + ":function:lambda1"),
                                new EventSourceMappingConfiguration()
                                        .withEventSourceArn("arn:aws:kinesis:" + TEST_REGION + ":" + TEST_ACCOUNT_ID + ":stream/stream2")
                                        .withFunctionArn("arn:aws:lambda:" + TEST_REGION + ":" + TEST_ACCOUNT_ID + ":function:lambda2")
                        ));
        when(cloudWatchClient.describeAlarms(new DescribeAlarmsRequest().withAlarmNamePrefix("Kinesis stream stream1 is")))
                .thenReturn(new DescribeAlarmsResult().withMetricAlarms(new MetricAlarm().withAlarmName("alarm1")));
        when(cloudWatchClient.describeAlarms(new DescribeAlarmsRequest().withAlarmNamePrefix("Kinesis stream stream2 is")))
                .thenReturn(new DescribeAlarmsResult().withMetricAlarms(new MetricAlarm().withAlarmName("alarm2")));
        when(kinesisClient.describeStream("stream1"))
                .thenReturn(new DescribeStreamResult().withStreamDescription(new StreamDescription()
                        .withStreamName("stream1")));
        when(kinesisClient.describeStream("stream2"))
                .thenReturn(new DescribeStreamResult().withStreamDescription(new StreamDescription()
                        .withStreamName("stream2")));
        when(kinesisClient.describeStream("stream3"))
                .thenThrow(new ResourceNotFoundException("stream3 not found"));

        List<String> resources = new ArrayList<>();
        resources.add("stream1");
        resources.add("stream2");
        resources.add("stream3");
        List<String> details = new ArrayList<>();
        Set<KinesisResource> actualResources = kinesisResourceFetcher.fetchResources(TEST_REGION, resources, details);

        verify(kinesisClient).describeStream("stream1");
        verify(kinesisClient).describeStream("stream2");
        verify(kinesisClient).describeStream("stream3");

        assertThat(actualResources.size(), is(equalTo(2)));
        assertThat(actualResources, hasItem(new KinesisResource()
                .setResourceName("stream1")
                .setCloudwatchAlarmList(new LinkedHashSet<String>() {{
                    add("alarm1");
                }})));
        assertThat(actualResources, hasItem(new KinesisResource()
                .setResourceName("stream2")
                .setCloudwatchAlarmList(new LinkedHashSet<String>() {{
                    add("alarm2");
                }})));

        assertThat(details.size(), is(equalTo(3)));
        assertThat(details, hasItem("Resources info for: [stream1], lambdas this stream triggers: [lambda1], cw alarms: [alarm1]"));
        assertThat(details, hasItem("Resources info for: [stream2], lambdas this stream triggers: [lambda2], cw alarms: [alarm2]"));
        assertThat(details, hasItem("!!! Kinesis stream not exists: stream3"));
    }

    @Test
    public void getUsage() throws Exception {
        when(cloudWatchClient.getMetricData(org.mockito.Mockito.any(GetMetricDataRequest.class)))
                .thenReturn(new GetMetricDataResult()
                        .withMetricDataResults(new MetricDataResult().withId("totalUsage").withValues(4.13)));
        Object usage = kinesisResourceFetcher.getUsage(TEST_REGION, "stream1", 7);
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
                                .withMetricName("GetRecords.Bytes")
                                .withDimensions(new Dimension()
                                        .withName("StreamName")
                                        .withValue("stream1")
                                )
                                .withNamespace("AWS/Kinesis")
                        )
                        .withPeriod(period)
                )));
        assertThat(actualRequest.getMetricDataQueries(), hasItem(new MetricDataQuery()
                .withId("m2")
                .withMetricStat(new MetricStat()
                        .withStat("Sum")
                        .withMetric(new Metric()
                                .withMetricName("IncomingBytes")
                                .withDimensions(new Dimension()
                                        .withName("StreamName")
                                        .withValue("stream1")
                                )
                                .withNamespace("AWS/Kinesis")
                        )
                        .withPeriod(period)
                )));
        assertThat(actualRequest.getMetricDataQueries(), hasItem(new MetricDataQuery()
                .withId("totalUsage")
                .withExpression("m1+m2")));
    }
}