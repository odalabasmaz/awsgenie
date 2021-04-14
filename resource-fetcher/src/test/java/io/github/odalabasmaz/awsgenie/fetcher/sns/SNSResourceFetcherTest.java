package io.github.odalabasmaz.awsgenie.fetcher.sns;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.*;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.ResourceNotFoundException;
import com.amazonaws.services.sns.model.*;
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
 * @version 13.04.2021
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        AWSClientProvider.class
})
@PowerMockIgnore({
        "javax.management.*", "javax.script.*"
})
public class SNSResourceFetcherTest {
    private static final String TEST_REGION = "us-west-2";
    private static final String TEST_ACCOUNT_ID = "111111111111";

    @Mock
    private ResourceFetcherConfiguration resourceFetcherConfiguration;

    @Mock
    private AmazonCloudWatch cloudWatchClient;

    @Mock
    private AmazonSNS snsClient;


    @Mock
    private AWSClientProvider awsClientProvider;

    private SNSResourceFetcher SNSResourceFetcher;

    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(AWSClientProvider.class);
        when(AWSClientProvider.getInstance(resourceFetcherConfiguration))
                .thenReturn(awsClientProvider);
        when(awsClientProvider.getAmazonCloudWatch())
                .thenReturn(cloudWatchClient);
        when(awsClientProvider.getAmazonSNS())
                .thenReturn(snsClient);

        this.SNSResourceFetcher = new SNSResourceFetcher(resourceFetcherConfiguration);

        when(snsClient.listTopics(new ListTopicsRequest()))
                .thenReturn(new ListTopicsResult()
                        .withTopics(
                                new Topic().withTopicArn("arn:aws:sns:" + TEST_REGION + ":" + TEST_ACCOUNT_ID + ":topic1"),
                                new Topic().withTopicArn("arn:aws:sns:" + TEST_REGION + ":" + TEST_ACCOUNT_ID + ":topic2"))
                        .withNextToken("nextToken"));
        when(snsClient.listTopics(new ListTopicsRequest().withNextToken("nextToken")))
                .thenReturn(new ListTopicsResult()
                        .withTopics(
                                new Topic().withTopicArn("arn:aws:sns:" + TEST_REGION + ":" + TEST_ACCOUNT_ID + ":topic3"),
                                new Topic().withTopicArn("arn:aws:sns:" + TEST_REGION + ":" + TEST_ACCOUNT_ID + ":topic4")));
    }

    @Test
    public void listResources() {
        List<String> actualTopics = new ArrayList<>();

        SNSResourceFetcher.listResources(TEST_REGION, actualTopics::addAll);

        verify(snsClient, times(2)).listTopics(org.mockito.Mockito.any(ListTopicsRequest.class));
        assertThat(actualTopics.size(), is(equalTo(4)));
        assertThat(actualTopics, hasItem("topic1"));
        assertThat(actualTopics, hasItem("topic2"));
        assertThat(actualTopics, hasItem("topic3"));
        assertThat(actualTopics, hasItem("topic4"));
    }

    @Test
    public void fetchResources() {
        String topicArn1 = "arn:aws:sns:" + TEST_REGION + ":" + TEST_ACCOUNT_ID + ":topic1";
        String topicArn2 = "arn:aws:sns:" + TEST_REGION + ":" + TEST_ACCOUNT_ID + ":topic2";
        String topicArn3 = "arn:aws:sns:" + TEST_REGION + ":" + TEST_ACCOUNT_ID + ":topic3";

        when(snsClient.listSubscriptionsByTopic(new ListSubscriptionsByTopicRequest(topicArn1, null)))
                .thenReturn(new ListSubscriptionsByTopicResult().withSubscriptions(
                        new Subscription().withEndpoint("arn:aws:lambda:" + TEST_REGION + ":" + TEST_ACCOUNT_ID + ":function:lambda1")
                                .withSubscriptionArn("arn:aws:sns:" + TEST_REGION + ":" + TEST_ACCOUNT_ID + ":topic1:subs1")
                ));
        when(snsClient.listSubscriptionsByTopic(new ListSubscriptionsByTopicRequest(topicArn2, null)))
                .thenReturn(new ListSubscriptionsByTopicResult().withSubscriptions(
                        new Subscription().withEndpoint("arn:aws:lambda:" + TEST_REGION + ":" + TEST_ACCOUNT_ID + ":function:lambda1")
                                .withSubscriptionArn("arn:aws:sns:" + TEST_REGION + ":" + TEST_ACCOUNT_ID + ":topic2:subs1")
                ));
        when(snsClient.listSubscriptionsByTopic(new ListSubscriptionsByTopicRequest(topicArn3, null)))
                .thenThrow(new ResourceNotFoundException("topic3 not found"));

        when(cloudWatchClient.describeAlarms(new DescribeAlarmsRequest().withAlarmNamePrefix("SNS Notification Failure-topic1-" + TEST_REGION)))
                .thenReturn(new DescribeAlarmsResult().withMetricAlarms(
                        new MetricAlarm().withAlarmName("alarm1")
                ));
        when(cloudWatchClient.describeAlarms(new DescribeAlarmsRequest().withAlarmNamePrefix("SNS Notification Failure-topic2-" + TEST_REGION)))
                .thenReturn(new DescribeAlarmsResult().withMetricAlarms(
                        new MetricAlarm().withAlarmName("alarm2")
                ));

        List<String> resources = new ArrayList<>();
        resources.add("topic1");
        resources.add("topic2");
        resources.add("topic3");
        List<String> details = new ArrayList<>();

        List<SNSResource> actualResources = SNSResourceFetcher.fetchResources(TEST_REGION, resources, details);
        assertThat(actualResources.size(), is(equalTo(2)));
        assertThat(actualResources, hasItem(new SNSResource()
                .setResourceName(topicArn1)
                .setCloudwatchAlarms(new LinkedHashSet<String>() {{
                    add("alarm1");
                }})));
        assertThat(actualResources, hasItem(new SNSResource()
                .setResourceName(topicArn2)
                .setCloudwatchAlarms(new LinkedHashSet<String>() {{
                    add("alarm2");
                }})));
        assertThat(details.size(), is(equalTo(3)));
        assertThat(details, hasItem("Resources info for: [topic1], subscriptions: [arn:aws:sns:" + TEST_REGION
                + ":" + TEST_ACCOUNT_ID + ":topic1:subs1], cw alarms: [alarm1]"));
        assertThat(details, hasItem("Resources info for: [topic2], subscriptions: [arn:aws:sns:" + TEST_REGION
                + ":" + TEST_ACCOUNT_ID + ":topic2:subs1], cw alarms: [alarm2]"));
        assertThat(details, hasItem("!!! Topic not exists: topic3"));

        verify(snsClient, times(2)).listTopics(org.mockito.Mockito.any(ListTopicsRequest.class));
        verify(snsClient, times(3)).listSubscriptionsByTopic(org.mockito.Mockito.any(ListSubscriptionsByTopicRequest.class));
        verify(cloudWatchClient, times(2)).describeAlarms(org.mockito.Mockito.any(DescribeAlarmsRequest.class));
    }

    @Test
    public void getUsage() throws Exception {
        String topicArn1 = "arn:aws:sns:" + TEST_REGION + ":" + TEST_ACCOUNT_ID + ":topic1";
        when(cloudWatchClient.getMetricData(org.mockito.Mockito.any(GetMetricDataRequest.class)))
                .thenReturn(new GetMetricDataResult()
                        .withMetricDataResults(new MetricDataResult().withId("m1").withValues(4.13)));
        Object usage = SNSResourceFetcher.getUsage(TEST_REGION, topicArn1, 7);
        assertThat(usage, is(equalTo(4.13)));

        ArgumentCaptor<GetMetricDataRequest> captor = ArgumentCaptor.forClass(GetMetricDataRequest.class);
        verify(cloudWatchClient).getMetricData(captor.capture());
        GetMetricDataRequest actualRequest = captor.getValue();
        Date endTime = actualRequest.getEndTime();
        Date startTime = actualRequest.getStartTime();
        Integer period = ((Long) TimeUnit.DAYS.toSeconds(7)).intValue();

        assertThat(startTime, is(equalTo(new Date(endTime.getTime() - TimeUnit.DAYS.toMillis(7)))));
        assertThat(actualRequest.getMaxDatapoints(), is(equalTo(100)));
        assertThat(actualRequest.getMetricDataQueries().size(), is(equalTo(1)));
        assertThat(actualRequest.getMetricDataQueries(), hasItem(new MetricDataQuery()
                .withId("m1")
                .withMetricStat(new MetricStat()
                        .withStat("Sum")
                        .withMetric(new Metric()
                                .withMetricName("NumberOfMessagesPublished")
                                .withDimensions(new Dimension()
                                        .withName("TopicName")
                                        .withValue("topic1")
                                )
                                .withNamespace("AWS/SNS")
                        )
                        .withPeriod(period)
                )));
    }
}