package com.atlassian.awstool.terminate.lambda;

import com.amazonaws.auth.policy.Condition;
import com.amazonaws.auth.policy.Policy;
import com.amazonaws.auth.policy.Resource;
import com.amazonaws.auth.policy.Statement;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.DescribeAlarmsRequest;
import com.amazonaws.services.cloudwatch.model.DescribeAlarmsResult;
import com.amazonaws.services.cloudwatch.model.MetricAlarm;
import com.amazonaws.services.cloudwatchevents.AmazonCloudWatchEvents;
import com.amazonaws.services.cloudwatchevents.model.ListTargetsByRuleRequest;
import com.amazonaws.services.cloudwatchevents.model.ListTargetsByRuleResult;
import com.amazonaws.services.cloudwatchevents.model.Target;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.*;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.ListSubscriptionsByTopicResult;
import com.amazonaws.services.sns.model.Subscription;
import com.atlassian.awstool.terminate.FetcherConfiguration;
import credentials.AwsClientProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
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
        AwsClientProvider.class
})
@PowerMockIgnore({
        "javax.management.*", "javax.script.*"
})
public class FetchLambdaResourcesTest {
    private static final String TEST_REGION = "us-west-2";
    private static final String TEST_ACCOUNT_ID = "111111111111";

    @Mock
    private FetcherConfiguration fetcherConfiguration;

    @Mock
    private AmazonCloudWatch cloudWatchClient;

    @Mock
    private AmazonCloudWatchEvents cloudWatchEventsClient;

    @Mock
    private AmazonSNS snsClient;

    @Mock
    private AWSLambda lambdaClient;

    @Mock
    private AwsClientProvider awsClientProvider;

    private FetchLambdaResources fetchLambdaResources;

    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(AwsClientProvider.class);
        when(AwsClientProvider.getInstance(fetcherConfiguration))
                .thenReturn(awsClientProvider);
        when(awsClientProvider.getAmazonCloudWatch())
                .thenReturn(cloudWatchClient);
        when(awsClientProvider.getAmazonCloudWatchEvents())
                .thenReturn(cloudWatchEventsClient);
        when(awsClientProvider.getAmazonLambda())
                .thenReturn(lambdaClient);
        when(awsClientProvider.getAmazonSNS())
                .thenReturn(snsClient);

        this.fetchLambdaResources = new FetchLambdaResources(fetcherConfiguration);
    }

    @Test
    public void listResources() {
        List<String> actualLambdas = new ArrayList<>();

        when(lambdaClient.listFunctions(new ListFunctionsRequest()))
                .thenReturn(new ListFunctionsResult()
                        .withFunctions(
                                new FunctionConfiguration().withFunctionName("lambda1"),
                                new FunctionConfiguration().withFunctionName("lambda2"))
                        .withNextMarker("nextMarker"));
        when(lambdaClient.listFunctions(new ListFunctionsRequest().withMarker("nextMarker")))
                .thenReturn(new ListFunctionsResult()
                        .withFunctions(
                                new FunctionConfiguration().withFunctionName("lambda3"),
                                new FunctionConfiguration().withFunctionName("lambda4")));

        fetchLambdaResources.listResources(TEST_REGION, actualLambdas::addAll);

        verify(lambdaClient, times(2)).listFunctions(org.mockito.Mockito.any(ListFunctionsRequest.class));
        assertThat(actualLambdas.size(), is(equalTo(4)));
        assertThat(actualLambdas, hasItem("lambda1"));
        assertThat(actualLambdas, hasItem("lambda2"));
        assertThat(actualLambdas, hasItem("lambda3"));
        assertThat(actualLambdas, hasItem("lambda4"));
    }

    @Test
    public void fetchResources() {
        when(lambdaClient.getFunction(new GetFunctionRequest().withFunctionName("lambda1")))
                .thenReturn(new GetFunctionResult().withConfiguration(new FunctionConfiguration().withFunctionArn("arn:aws:lambda:" + TEST_REGION + ":" + TEST_ACCOUNT_ID + ":function:lambda1")));
        when(lambdaClient.getFunction(new GetFunctionRequest().withFunctionName("lambda2")))
                .thenThrow(new ResourceNotFoundException("lambda2 not found"));
        when(lambdaClient.listEventSourceMappings(new ListEventSourceMappingsRequest().withFunctionName("lambda1")))
                .thenReturn(new ListEventSourceMappingsResult().withEventSourceMappings(
                        new EventSourceMappingConfiguration()
                                .withEventSourceArn("arn:aws:kinesis:" + TEST_REGION + ":" + TEST_ACCOUNT_ID + ":stream/stream1")
                                .withFunctionArn("arn:aws:lambda:" + TEST_REGION + ":" + TEST_ACCOUNT_ID + ":function:lambda1")
                                .withUUID("uuid1"),
                        new EventSourceMappingConfiguration()
                                .withEventSourceArn("arn:aws:kinesis:" + TEST_REGION + ":" + TEST_ACCOUNT_ID + ":stream/stream2")
                                .withFunctionArn("arn:aws:lambda:" + TEST_REGION + ":" + TEST_ACCOUNT_ID + ":function:lambda1")
                                .withUUID("uuid2")
                ));

        when(snsClient.listSubscriptionsByTopic("arn:aws:sns:" + TEST_REGION + ":" + TEST_ACCOUNT_ID + ":topic1"))
                .thenReturn(new ListSubscriptionsByTopicResult().withSubscriptions(
                        new Subscription().withEndpoint("arn:aws:lambda:" + TEST_REGION + ":" + TEST_ACCOUNT_ID + ":function:lambda1")
                                .withSubscriptionArn("arn:aws:sns:" + TEST_REGION + ":" + TEST_ACCOUNT_ID + ":topic1:subs1"),
                        new Subscription().withEndpoint("arn:aws:lambda:" + TEST_REGION + ":" + TEST_ACCOUNT_ID + ":function:lambda2")
                ));
        when(cloudWatchEventsClient.listTargetsByRule(new ListTargetsByRuleRequest().withRule("rule1")))
                .thenReturn(new ListTargetsByRuleResult().withTargets(
                        new Target()
                                .withId("target1")
                                .withArn("arn:aws:lambda:" + TEST_REGION + ":" + TEST_ACCOUNT_ID + ":function:lambda1")
                ));

        Policy policy = new Policy()
                .withId("policy1")
                .withStatements(
                        new Statement(Statement.Effect.Allow)
                                .withId("statement1")
                                .withActions(() -> "lambda:InvokeFunction")
                                .withResources(new Resource("arn:aws:lambda:" + TEST_REGION + ":" + TEST_ACCOUNT_ID + ":function:lambda1"))
                                .withConditions(
                                        new Condition()
                                                .withConditionKey("aws:arn")
                                                .withType("ArnEquals")
                                                .withValues("arn:aws:sns:" + TEST_REGION + ":" + TEST_ACCOUNT_ID + ":topic1")
                                ),
                        new Statement(Statement.Effect.Allow)
                        .withId("statement2")
                        .withActions(() -> "lambda:InvokeFunction")
                        .withResources(new Resource("arn:aws:lambda:" + TEST_REGION + ":" + TEST_ACCOUNT_ID + ":function:lambda1"))
                        .withConditions(
                                new Condition()
                                        .withConditionKey("aws:arn")
                                        .withType("ArnEquals")
                                        .withValues("arn:aws:events:" + TEST_REGION + ":" + TEST_ACCOUNT_ID + ":rule/rule1")
                        )
                );

        when(lambdaClient.getPolicy(new GetPolicyRequest().withFunctionName("lambda1")))
                .thenReturn(new GetPolicyResult().withPolicy(policy.toJson()));

        when(cloudWatchClient.describeAlarms(new DescribeAlarmsRequest().withAlarmNamePrefix(TEST_REGION + " lambda1 Lambda ")))
                .thenReturn(new DescribeAlarmsResult().withMetricAlarms(
                        new MetricAlarm().withAlarmName("alarm1"),
                        new MetricAlarm().withAlarmName("alarm2")
                ));

        List<String> resources = new ArrayList<>();
        resources.add("lambda1");
        resources.add("lambda2");
        List<String> details = new ArrayList<>();

        List<LambdaResource> actualResources = fetchLambdaResources.fetchResources(TEST_REGION, resources, details);

        assertThat(actualResources.size(), is(equalTo(1)));
        LambdaResource lambdaResource = actualResources.get(0);
        assertThat(lambdaResource.getCloudwatchAlarms().size(), is(equalTo(2)));
        assertThat(lambdaResource.getCloudwatchAlarms(), hasItem("alarm1"));
        assertThat(lambdaResource.getCloudwatchAlarms(), hasItem("alarm2"));
        assertThat(lambdaResource.getEventSourceMappings().size(), is(equalTo(2)));
        assertThat(lambdaResource.getEventSourceMappings(), hasItem("uuid1"));
        assertThat(lambdaResource.getEventSourceMappings(), hasItem("uuid2"));
        assertThat(lambdaResource.getCloudwatchRules().size(), is(equalTo(1)));
        assertThat(lambdaResource.getCloudwatchRules(), hasItem("rule1"));
        assertThat(lambdaResource.getCloudwatchRuleTargets().size(), is(equalTo(1)));
        assertThat(lambdaResource.getCloudwatchRuleTargets(), hasItem("rule1:target1"));
        assertThat(lambdaResource.getSnsTriggers().size(), is(equalTo(1)));
        assertThat(lambdaResource.getSnsTriggers(), hasItem("arn:aws:sns:" + TEST_REGION + ":" + TEST_ACCOUNT_ID + ":topic1:subs1"));

        verify(lambdaClient, times(2)).getFunction(org.mockito.Mockito.any(GetFunctionRequest.class));
        verify(lambdaClient).listEventSourceMappings(org.mockito.Mockito.any(ListEventSourceMappingsRequest.class));
        verify(lambdaClient).getPolicy(org.mockito.Mockito.any(GetPolicyRequest.class));
        verify(snsClient).listSubscriptionsByTopic(org.mockito.Mockito.any(String.class));
        verify(cloudWatchEventsClient).listTargetsByRule(org.mockito.Mockito.any(ListTargetsByRuleRequest.class));
        verify(cloudWatchClient).describeAlarms(org.mockito.Mockito.any(DescribeAlarmsRequest.class));
    }
}