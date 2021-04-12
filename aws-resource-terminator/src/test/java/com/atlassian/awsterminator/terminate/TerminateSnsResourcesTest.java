package com.atlassian.awsterminator.terminate;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.DeleteAlarmsRequest;
import com.amazonaws.services.sns.AmazonSNS;
import com.atlassian.awsterminator.interceptor.AfterTerminateInterceptor;
import com.atlassian.awsterminator.interceptor.BeforeTerminateInterceptor;
import com.atlassian.awsterminator.interceptor.InterceptorRegistry;
import com.atlassian.awstool.terminate.AWSResource;
import com.atlassian.awstool.terminate.FetchResourceFactory;
import com.atlassian.awstool.terminate.FetchResources;
import com.atlassian.awstool.terminate.Service;
import com.atlassian.awstool.terminate.sns.SNSResource;
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

public class TerminateSnsResourcesTest extends TerminatorTest {
    private static final String TEST_REGION = "us-west-2";
    private static final String TEST_TICKET = "TEST-TICKET";
    private final Service service = Service.SNS;

    public static final String TOPIC_1 = "topic1";
    public static final String TOPIC_2 = "topic2";
    private static final List<String> TEST_RESOURCES = new ArrayList<String>() {{
        add(TOPIC_1);
        add(TOPIC_2);
        add("topic3");
    }};

    private static final List<AWSResource> TEST_FETCHED_RESOURCES = new ArrayList<AWSResource>() {{
        add(new SNSResource()
                .setResourceName(TOPIC_1)
                .setCloudwatchAlarms(new LinkedHashSet<String>() {{
                    add("SNS Notification Failure-topic1");
                }}));
        add(new SNSResource()
                .setResourceName(TOPIC_2)
                .setCloudwatchAlarms(new LinkedHashSet<String>() {{
                    add("SNS Notification Failure-topic2");
                }}));
    }};
    @Mock
    private BeforeTerminateInterceptor beforeTerminateInterceptor;

    @Mock
    private AfterTerminateInterceptor afterTerminateInterceptor;

    private TerminateSnsResources terminateSnsResources;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        InterceptorRegistry.getBeforeTerminateInterceptors().clear();
        InterceptorRegistry.getAfterTerminateInterceptors().clear();
        this.terminateSnsResources = new TerminateSnsResources(TerminatorHelper.getRegion1Account1Configuration());
        this.terminateSnsResources.setFetchResourceFactory(getFetchResourceFactory());

        FetchResources fetchResources = getFetchResources();
        doReturn(TEST_FETCHED_RESOURCES)
                .when(fetchResources).fetchResources(eq(TEST_REGION), eq(TEST_RESOURCES), org.mockito.Mockito.any(List.class));
        doReturn(0.0)
                .when(fetchResources).getUsage(eq(TEST_REGION), eq(TOPIC_1));
        doReturn(1.0)
                .when(fetchResources).getUsage(eq(TEST_REGION), eq(TOPIC_2));
    }

    @Test
    public void terminateResourcesWithApply() throws Exception {
        terminateSnsResources.terminateResource(TEST_REGION, service, TEST_RESOURCES, TEST_TICKET, true);

        AmazonSNS amazonSNS = getAmazonSNS();
        verify(amazonSNS).deleteTopic("topic1");
        verifyNoMoreInteractions(amazonSNS);

        ArgumentCaptor<DeleteAlarmsRequest> cwCaptor = ArgumentCaptor.forClass(DeleteAlarmsRequest.class);
        verify(getCloudWatchClient()).deleteAlarms(cwCaptor.capture());
        verifyNoMoreInteractions(getCloudWatchClient());
        DeleteAlarmsRequest actualCWRequest = cwCaptor.getValue();
        assertThat(actualCWRequest.getAlarmNames().size(), is(equalTo(1)));
        assertThat(actualCWRequest.getAlarmNames(), hasItem("SNS Notification Failure-topic1"));
    }

    @Test
    public void terminateResourcesWithoutApply() throws Exception {
        terminateSnsResources.terminateResource(TEST_REGION, service, TEST_RESOURCES, TEST_TICKET, false);
        verifyZeroInteractions(getCloudWatchClient());
        verifyZeroInteractions(getAmazonSNS());
    }

    @Test
    public void interceptorsAreCalled() throws Exception {
        InterceptorRegistry.addInterceptor(beforeTerminateInterceptor);
        InterceptorRegistry.addInterceptor(afterTerminateInterceptor);
        terminateSnsResources.terminateResource(TEST_REGION, service, TEST_RESOURCES, TEST_TICKET, false);
        verify(beforeTerminateInterceptor).intercept(eq(service), eq(TEST_FETCHED_RESOURCES), org.mockito.Mockito.any(String.class), eq(false));
        verify(afterTerminateInterceptor).intercept(eq(service), eq(TEST_FETCHED_RESOURCES), org.mockito.Mockito.any(String.class), eq(false));
    }
}