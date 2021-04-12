package com.atlassian.awsterminator.terminate;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.DeleteAlarmsRequest;
import com.amazonaws.services.cloudwatchevents.AmazonCloudWatchEvents;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.DeleteEventSourceMappingRequest;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sqs.AmazonSQS;
import com.atlassian.awsterminator.interceptor.AfterTerminateInterceptor;
import com.atlassian.awsterminator.interceptor.BeforeTerminateInterceptor;
import com.atlassian.awsterminator.interceptor.InterceptorRegistry;
import com.atlassian.awstool.terminate.AWSResource;
import com.atlassian.awstool.terminate.FetchResourceFactory;
import com.atlassian.awstool.terminate.FetchResources;
import com.atlassian.awstool.terminate.Service;
import com.atlassian.awstool.terminate.sqs.SQSResource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;


/**
 * @author Celal Emre CICEK
 * @version 9.04.2021
 */

public class TerminateSqsResourcesTest extends TerminatorTest {
    private static final String TEST_REGION = "us-west-2";
    private static final String TEST_TICKET = "TEST-TICKET";
    private final Service service = Service.SQS;

    private static final List<String> TEST_RESOURCES = new ArrayList<String>() {{
        add("queue1");
        add("queue2");
        add("queue3");
    }};

    private static final List<AWSResource> TEST_FETCHED_RESOURCES = new ArrayList<AWSResource>() {{
        add(new SQSResource()
                .setResourceName("queue1")
                .setCloudwatchAlarms(new LinkedHashSet<String>() {{
                    add("SQS Queue queue1");
                }})
                .setLambdaTriggers(new LinkedHashSet<String>() {{
                    add("lambda1");
                }})
                .setSnsSubscriptions(new LinkedHashSet<String>() {{
                    add("sns1");
                }}));
        add(new SQSResource()
                .setResourceName("queue2")
                .setCloudwatchAlarms(new LinkedHashSet<String>() {{
                    add("SQS Queue queue2");
                }}));
    }};

    @Mock
    private BeforeTerminateInterceptor beforeTerminateInterceptor;

    @Mock
    private AfterTerminateInterceptor afterTerminateInterceptor;

    private TerminateSqsResources terminateSqsResources;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        InterceptorRegistry.getBeforeTerminateInterceptors().clear();
        InterceptorRegistry.getAfterTerminateInterceptors().clear();
        this.terminateSqsResources = new TerminateSqsResources(TerminatorHelper.getRegion1Account1Configuration());
        terminateSqsResources.setFetchResourceFactory(getFetchResourceFactory());
        doReturn(TEST_FETCHED_RESOURCES)
                .when(getFetchResources()).fetchResources(eq(TEST_REGION), eq(TEST_RESOURCES), org.mockito.Mockito.any(List.class));
    }

    @Test
    public void terminateResourcesWithApply() throws Exception {
        terminateSqsResources.terminateResource(TEST_REGION, service, TEST_RESOURCES, TEST_TICKET, true);
        AmazonSNS snsClient = getAmazonSNS();
        AWSLambda lambdaClient = getAmazonLambda();
        AmazonSQS sqsClient = getAmazonSQS();
        AmazonCloudWatch cloudWatchClient = getCloudWatchClient();
        verify(lambdaClient).deleteEventSourceMapping(new DeleteEventSourceMappingRequest().withUUID("lambda1"));
        verifyNoMoreInteractions(lambdaClient);
        verify(snsClient).unsubscribe("sns1");
        verifyNoMoreInteractions(snsClient);
        verify(sqsClient).deleteQueue("queue1");
        verify(sqsClient).deleteQueue("queue2");
        verifyNoMoreInteractions(sqsClient);

        ArgumentCaptor<DeleteAlarmsRequest> cwCaptor = ArgumentCaptor.forClass(DeleteAlarmsRequest.class);
        verify(cloudWatchClient).deleteAlarms(cwCaptor.capture());
        verifyNoMoreInteractions(cloudWatchClient);
        DeleteAlarmsRequest actualCWRequest = cwCaptor.getValue();
        assertThat(actualCWRequest.getAlarmNames().size(), is(equalTo(2)));
        assertThat(actualCWRequest.getAlarmNames(), hasItem("SQS Queue queue1"));
        assertThat(actualCWRequest.getAlarmNames(), hasItem("SQS Queue queue2"));
    }

    @Test
    public void terminateResourcesWithoutApply() throws Exception {
        terminateSqsResources.terminateResource(TEST_REGION, service, TEST_RESOURCES, TEST_TICKET, false);
        AmazonSNS snsClient = getAmazonSNS();
        AWSLambda lambdaClient = getAmazonLambda();
        AmazonSQS sqsClient = getAmazonSQS();
        AmazonCloudWatch cloudWatchClient = getCloudWatchClient();
        verifyZeroInteractions(cloudWatchClient);
        verifyZeroInteractions(snsClient);
        verifyZeroInteractions(sqsClient);
        verifyZeroInteractions(lambdaClient);
    }

    @Test
    public void interceptorsAreCalled() throws Exception {
        InterceptorRegistry.addInterceptor(beforeTerminateInterceptor);
        InterceptorRegistry.addInterceptor(afterTerminateInterceptor);
        terminateSqsResources.terminateResource(TEST_REGION, service, TEST_RESOURCES, TEST_TICKET, false);
        verify(beforeTerminateInterceptor).intercept(eq(service), eq(TEST_FETCHED_RESOURCES), org.mockito.Mockito.any(String.class), eq(false));
        verify(afterTerminateInterceptor).intercept(eq(service), eq(TEST_FETCHED_RESOURCES), org.mockito.Mockito.any(String.class), eq(false));
    }
}