package com.atlassian.awsterminator.terminate;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.DeleteAlarmsRequest;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.atlassian.awsterminator.interceptor.AfterTerminateInterceptor;
import com.atlassian.awsterminator.interceptor.BeforeTerminateInterceptor;
import com.atlassian.awsterminator.interceptor.InterceptorRegistry;
import com.atlassian.awstool.terminate.AWSResource;
import com.atlassian.awstool.terminate.FetchResourceFactory;
import com.atlassian.awstool.terminate.FetchResources;
import com.atlassian.awstool.terminate.Service;
import com.atlassian.awstool.terminate.kinesis.KinesisResource;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;


/**
 * @author Celal Emre CICEK
 * @version 9.04.2021
 */

@RunWith(MockitoJUnitRunner.class)
public class TerminateKinesisResourcesTest {
    private static final String TEST_REGION = "us-west-2";
    private static final String TEST_TICKET = "TEST-TICKET";
    private final Service service = Service.KINESIS;

    public static final String STREAM_1 = "stream1";
    public static final String STREAM_2 = "stream2";
    public static final String STREAM_3 = "stream3";
    private static final List<String> TEST_RESOURCES = new ArrayList<String>() {{
        add(STREAM_1);
        add(STREAM_2);
        add(STREAM_3);
        add("stream4");
    }};

    private static final List<AWSResource> TEST_FETCHED_RESOURCES = new ArrayList<AWSResource>() {{
        add(new KinesisResource()
                .setResourceName(STREAM_1)
                .setCloudwatchAlarmList(new LinkedHashSet<String>() {{
                    add("Kinesis stream stream1 is Read throttled.");
                    add("Kinesis stream stream1 is Write throttled.");
                }}));
        add(new KinesisResource()
                .setResourceName(STREAM_2)
                .setCloudwatchAlarmList(new LinkedHashSet<String>() {{
                    add("Kinesis stream stream2 is Read throttled.");
                }}));
        add(new KinesisResource()
                .setResourceName(STREAM_3)
                .setCloudwatchAlarmList(new LinkedHashSet<String>() {{
                    add("Kinesis stream stream3 is Read throttled.");
                    add("Kinesis stream stream3 is Write throttled.");
                }}));
    }};

    @Mock
    private AmazonCloudWatch cloudWatchClient;

    @Mock
    private AmazonKinesis kinesisClient;

    @Mock
    private AWSCredentialsProvider credentialsProvider;

    @Mock
    private FetchResourceFactory fetchResourceFactory;

    @Mock
    private FetchResources fetchResources;

    @Mock
    private BeforeTerminateInterceptor beforeTerminateInterceptor;

    @Mock
    private AfterTerminateInterceptor afterTerminateInterceptor;

    private TerminateKinesisResources terminateKinesisResources;

    @Before
    public void setUp() throws Exception {
        InterceptorRegistry.getBeforeTerminateInterceptors().clear();
        InterceptorRegistry.getAfterTerminateInterceptors().clear();
        this.terminateKinesisResources = new TerminateKinesisResources(credentialsProvider);
        terminateKinesisResources.setCloudWatchClient(cloudWatchClient);
        terminateKinesisResources.setKinesisClient(kinesisClient);
        terminateKinesisResources.setFetchResourceFactory(fetchResourceFactory);

        when(fetchResourceFactory.getFetcher(service, credentialsProvider))
                .thenReturn(fetchResources);
        doReturn(TEST_FETCHED_RESOURCES)
                .when(fetchResources).fetchResources(eq(TEST_REGION), eq(TEST_RESOURCES), org.mockito.Mockito.any(List.class));
        doReturn(0.0)
                .when(fetchResources).getUsage(eq(TEST_REGION), eq(STREAM_1));
        doReturn(0.0)
                .when(fetchResources).getUsage(eq(TEST_REGION), eq(STREAM_2));
        doReturn(1.0)
                .when(fetchResources).getUsage(eq(TEST_REGION), eq(STREAM_3));
    }

    @Test
    public void terminateResourcesWithApply() throws Exception {
        terminateKinesisResources.terminateResource(TEST_REGION, service, TEST_RESOURCES, TEST_TICKET, true);

        verify(kinesisClient).deleteStream("stream1");
        verify(kinesisClient).deleteStream("stream2");
        verifyNoMoreInteractions(kinesisClient);

        ArgumentCaptor<DeleteAlarmsRequest> captor = ArgumentCaptor.forClass(DeleteAlarmsRequest.class);
        verify(cloudWatchClient).deleteAlarms(captor.capture());
        verifyNoMoreInteractions(cloudWatchClient);
        DeleteAlarmsRequest actualRequest = captor.getValue();
        assertThat(actualRequest.getAlarmNames(), hasItem("Kinesis stream stream1 is Read throttled."));
        assertThat(actualRequest.getAlarmNames(), hasItem("Kinesis stream stream1 is Write throttled."));
        assertThat(actualRequest.getAlarmNames(), hasItem("Kinesis stream stream2 is Read throttled."));
        assertThat(actualRequest.getAlarmNames(), not(hasItem("Kinesis stream stream3 is Read throttled.")));
        assertThat(actualRequest.getAlarmNames(), not(hasItem("Kinesis stream stream3 is Write throttled.")));
    }

    @Test
    public void terminateResourcesWithoutApply() throws Exception {
        terminateKinesisResources.terminateResource(TEST_REGION, service, TEST_RESOURCES, TEST_TICKET, false);
        verifyZeroInteractions(kinesisClient);
        verifyZeroInteractions(cloudWatchClient);
    }

    @Test
    public void interceptorsAreCalled() throws Exception {
        InterceptorRegistry.addInterceptor(beforeTerminateInterceptor);
        InterceptorRegistry.addInterceptor(afterTerminateInterceptor);
        terminateKinesisResources.terminateResource(TEST_REGION, service, TEST_RESOURCES, TEST_TICKET, false);
        verify(beforeTerminateInterceptor).intercept(eq(service), eq(TEST_FETCHED_RESOURCES), any(String.class), eq(false));
        verify(afterTerminateInterceptor).intercept(eq(service), eq(TEST_FETCHED_RESOURCES), any(String.class), eq(false));
    }
}