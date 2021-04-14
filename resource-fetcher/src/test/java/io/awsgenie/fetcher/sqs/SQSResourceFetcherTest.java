package io.awsgenie.fetcher.sqs;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.DescribeAlarmsRequest;
import com.amazonaws.services.cloudwatch.model.DescribeAlarmsResult;
import com.amazonaws.services.cloudwatch.model.MetricAlarm;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.EventSourceMappingConfiguration;
import com.amazonaws.services.lambda.model.ListEventSourceMappingsRequest;
import com.amazonaws.services.lambda.model.ListEventSourceMappingsResult;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.ListSubscriptionsRequest;
import com.amazonaws.services.sns.model.ListSubscriptionsResult;
import com.amazonaws.services.sns.model.Subscription;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.*;
import io.awsgenie.fetcher.ResourceFetcherConfiguration;
import io.awsgenie.fetcher.credentials.AWSClientProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;

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
public class SQSResourceFetcherTest {
    private static final String TEST_REGION = "us-west-2";
    private static final String TEST_ACCOUNT_ID = "111111111111";

    @Mock
    private ResourceFetcherConfiguration resourceFetcherConfiguration;

    @Mock
    private AmazonCloudWatch cloudWatchClient;

    @Mock
    private AmazonSQS sqsClient;

    @Mock
    private AmazonSNS snsClient;

    @Mock
    private AWSLambda lambdaClient;

    @Mock
    private AWSClientProvider awsClientProvider;

    private SQSResourceFetcher SQSResourceFetcher;

    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(AWSClientProvider.class);
        when(AWSClientProvider.getInstance(resourceFetcherConfiguration))
                .thenReturn(awsClientProvider);
        when(awsClientProvider.getAmazonCloudWatch())
                .thenReturn(cloudWatchClient);
        when(awsClientProvider.getAmazonSQS())
                .thenReturn(sqsClient);
        when(awsClientProvider.getAmazonLambda())
                .thenReturn(lambdaClient);
        when(awsClientProvider.getAmazonSNS())
                .thenReturn(snsClient);

        this.SQSResourceFetcher = new SQSResourceFetcher(resourceFetcherConfiguration);

        when(sqsClient.listQueues(new ListQueuesRequest()))
                .thenReturn(new ListQueuesResult()
                        .withQueueUrls("queue1", "queue2")
                        .withNextToken("nextToken"));
        when(sqsClient.listQueues(new ListQueuesRequest().withNextToken("nextToken")))
                .thenReturn(new ListQueuesResult()
                        .withQueueUrls("queue3", "queue4"));
    }

    @Test
    public void listResources() {
        List<String> actualQueues = new ArrayList<>();
        SQSResourceFetcher.listResources(TEST_REGION, actualQueues::addAll);

        verify(sqsClient, times(2)).listQueues(org.mockito.Mockito.any(ListQueuesRequest.class));
        assertThat(actualQueues.size(), is(equalTo(4)));
        assertThat(actualQueues, hasItem("queue1"));
        assertThat(actualQueues, hasItem("queue2"));
        assertThat(actualQueues, hasItem("queue3"));
        assertThat(actualQueues, hasItem("queue4"));
    }

    @Test
    public void fetchResources() {
        when(snsClient.listSubscriptions(new ListSubscriptionsRequest()))
                .thenReturn(new ListSubscriptionsResult().withSubscriptions(
                        new Subscription()
                                .withEndpoint("arn:aws:sqs:" + TEST_REGION + ":" + TEST_ACCOUNT_ID + ":queue1")
                                .withProtocol("sqs")
                                .withSubscriptionArn("arn:aws:sns:" + TEST_REGION + ":" + TEST_ACCOUNT_ID + ":topic1:subs1"),
                        new Subscription()
                                .withEndpoint("arn:aws:sqs:" + TEST_REGION + ":" + TEST_ACCOUNT_ID + ":queue2")
                                .withProtocol("sqs")
                                .withSubscriptionArn("arn:aws:sns:" + TEST_REGION + ":" + TEST_ACCOUNT_ID + ":topic1:subs2")
                ));
        when(sqsClient.getQueueUrl(new GetQueueUrlRequest().withQueueName("queue1")))
                .thenReturn(new GetQueueUrlResult().withQueueUrl("queue1"));
        when(sqsClient.getQueueUrl(new GetQueueUrlRequest().withQueueName("queue2")))
                .thenReturn(new GetQueueUrlResult().withQueueUrl("queue2"));
        when(sqsClient.getQueueUrl(new GetQueueUrlRequest().withQueueName("queue3")))
                .thenThrow(new QueueDoesNotExistException("queue3 does not exist"));
        when(sqsClient.getQueueAttributes(new GetQueueAttributesRequest().withQueueUrl("queue1").withAttributeNames("All")))
                .thenReturn(new GetQueueAttributesResult().withAttributes(new HashMap<String, String>() {{
                    put("ApproximateNumberOfMessages", "10");
                    put("QueueArn", "queueArn1");
                }}));
        when(sqsClient.getQueueAttributes(new GetQueueAttributesRequest().withQueueUrl("queue2").withAttributeNames("All")))
                .thenReturn(new GetQueueAttributesResult().withAttributes(new HashMap<String, String>() {{
                    put("ApproximateNumberOfMessages", "20");
                    put("QueueArn", "queueArn2");
                }}));

        when(cloudWatchClient.describeAlarms(new DescribeAlarmsRequest().withAlarmNamePrefix("SQS Queue queue1 ")))
                .thenReturn(new DescribeAlarmsResult().withMetricAlarms(
                        new MetricAlarm().withAlarmName("alarm1")
                ));
        when(cloudWatchClient.describeAlarms(new DescribeAlarmsRequest().withAlarmNamePrefix("SQS Queue queue2 ")))
                .thenReturn(new DescribeAlarmsResult().withMetricAlarms(
                        new MetricAlarm().withAlarmName("alarm2")
                ));

        when(lambdaClient.listEventSourceMappings(org.mockito.Mockito.any(ListEventSourceMappingsRequest.class)))
                .thenReturn(new ListEventSourceMappingsResult().withEventSourceMappings(
                        new EventSourceMappingConfiguration()
                                .withEventSourceArn("arn:aws:sqs:" + TEST_REGION + ":" + TEST_ACCOUNT_ID + ":queue1")
                                .withFunctionArn("arn:aws:lambda:" + TEST_REGION + ":" + TEST_ACCOUNT_ID + ":function:lambda1")
                                .withUUID("uuid1")
                ))
                .thenReturn(new ListEventSourceMappingsResult());

        List<String> resources = new ArrayList<>();
        resources.add("queue1");
        resources.add("queue2");
        resources.add("queue3");
        List<String> details = new ArrayList<>();

        List<SQSResource> actualResources = SQSResourceFetcher.fetchResources(TEST_REGION, resources, details);

        assertThat(actualResources.size(), is(equalTo(2)));
        assertThat(actualResources, hasItem(new SQSResource()
                .setResourceName("queue1")
                .setCloudwatchAlarms(new LinkedHashSet<String>() {{
                    add("alarm1");
                }})
                .setLambdaTriggers(new LinkedHashSet<String>() {{
                    add("uuid1");
                }})
                .setSnsSubscriptions(new LinkedHashSet<String>() {{
                    add("arn:aws:sns:" + TEST_REGION + ":" + TEST_ACCOUNT_ID + ":topic1:subs1");
                }})
        ));
        assertThat(actualResources, hasItem(new SQSResource()
                .setResourceName("queue2")
                .setCloudwatchAlarms(new LinkedHashSet<String>() {{
                    add("alarm2");
                }})
                .setSnsSubscriptions(new LinkedHashSet<String>() {{
                    add("arn:aws:sns:" + TEST_REGION + ":" + TEST_ACCOUNT_ID + ":topic1:subs2");
                }})
        ));
        assertThat(details.size(), is(equalTo(3)));
        assertThat(details, hasItem("Resources info for: [queue1], there are [10] message(s) in queue, " +
                "sns subscription(s): [arn:aws:sns:" + TEST_REGION + ":" + TEST_ACCOUNT_ID + ":topic1:subs1], " +
                "lambda trigger(s): [lambda1], cw alarms: [alarm1]"));
        assertThat(details, hasItem("Resources info for: [queue2], there are [20] message(s) in queue, " +
                "sns subscription(s): [arn:aws:sns:" + TEST_REGION + ":" + TEST_ACCOUNT_ID + ":topic1:subs2], " +
                "lambda trigger(s): [], cw alarms: [alarm2]"));
        assertThat(details, hasItem("!!! SQS resource not exists: queue3"));

        verify(sqsClient).getQueueUrl(new GetQueueUrlRequest().withQueueName("queue1"));
        verify(sqsClient).getQueueUrl(new GetQueueUrlRequest().withQueueName("queue2"));
        verify(sqsClient).getQueueUrl(new GetQueueUrlRequest().withQueueName("queue3"));
        verify(sqsClient, times(2)).getQueueAttributes(org.mockito.Mockito.any(GetQueueAttributesRequest.class));
        verify(snsClient).listSubscriptions(org.mockito.Mockito.any(ListSubscriptionsRequest.class));
        verify(cloudWatchClient, times(2)).describeAlarms(org.mockito.Mockito.any(DescribeAlarmsRequest.class));
        verify(lambdaClient, times(2)).listEventSourceMappings(org.mockito.Mockito.any(ListEventSourceMappingsRequest.class));
    }
}