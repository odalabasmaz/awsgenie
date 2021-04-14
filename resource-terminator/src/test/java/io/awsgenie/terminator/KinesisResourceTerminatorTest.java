package io.awsgenie.terminator;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.DeleteAlarmsRequest;
import com.amazonaws.services.kinesis.AmazonKinesis;
import io.awsgenie.fetcher.Resource;
import io.awsgenie.fetcher.ResourceFetcher;
import io.awsgenie.fetcher.Service;
import io.awsgenie.fetcher.kinesis.KinesisResource;
import io.awsgenie.terminator.interceptor.AfterTerminateInterceptor;
import io.awsgenie.terminator.interceptor.BeforeTerminateInterceptor;
import io.awsgenie.terminator.interceptor.InterceptorRegistry;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;


/**
 * @author Celal Emre CICEK
 * @version 9.04.2021
 */

public class KinesisResourceTerminatorTest extends TerminatorTest {
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

    private static final List<Resource> TEST_FETCHED_RESOURCES = new ArrayList<Resource>() {{
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
    private BeforeTerminateInterceptor beforeTerminateInterceptor;

    @Mock
    private AfterTerminateInterceptor afterTerminateInterceptor;

    private KinesisResourceTerminator kinesisResourceTerminator;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        InterceptorRegistry.getBeforeTerminateInterceptors().clear();
        InterceptorRegistry.getAfterTerminateInterceptors().clear();
        this.kinesisResourceTerminator = new KinesisResourceTerminator(TerminatorHelper.getRegion1Account1Configuration());
        this.kinesisResourceTerminator.setFetchResourceFactory(getFetchResourceFactory());
        ResourceFetcher resourceFetcher = getFetchResources();
        doReturn(TEST_FETCHED_RESOURCES)
                .when(resourceFetcher).fetchResources( eq(TEST_RESOURCES), org.mockito.Mockito.any(List.class));
        doReturn(0.0)
                .when(resourceFetcher).getUsage(eq(TEST_REGION), eq(STREAM_1), eq(7));
        doReturn(0.0)
                .when(resourceFetcher).getUsage(eq(TEST_REGION), eq(STREAM_2), eq(7));
        doReturn(1.0)
                .when(resourceFetcher).getUsage(eq(TEST_REGION), eq(STREAM_3), eq(7));
    }

    @Test
    public void terminateResourcesWithApply() throws Exception {
        kinesisResourceTerminator.terminateResource(TEST_REGION, service, TEST_RESOURCES, TEST_TICKET, true);

        AmazonKinesis kinesisClient = getAmazonKinesis();
        verify(kinesisClient).deleteStream("stream1");
        verify(kinesisClient).deleteStream("stream2");
        verifyNoMoreInteractions(kinesisClient);

        ArgumentCaptor<DeleteAlarmsRequest> captor = ArgumentCaptor.forClass(DeleteAlarmsRequest.class);
        AmazonCloudWatch cloudWatchClient = getCloudWatchClient();
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
        kinesisResourceTerminator.terminateResource(TEST_REGION, service, TEST_RESOURCES, TEST_TICKET, false);
        verifyZeroInteractions(getAmazonKinesis());
        verifyZeroInteractions(getCloudWatchClient());
    }

    @Test
    public void interceptorsAreCalled() throws Exception {
        InterceptorRegistry.addInterceptor(beforeTerminateInterceptor);
        InterceptorRegistry.addInterceptor(afterTerminateInterceptor);
        kinesisResourceTerminator.terminateResource(TEST_REGION, service, TEST_RESOURCES, TEST_TICKET, false);
        verify(beforeTerminateInterceptor).intercept(eq(service), eq(TEST_FETCHED_RESOURCES), any(String.class), eq(false));
        verify(afterTerminateInterceptor).intercept(eq(service), eq(TEST_FETCHED_RESOURCES), any(String.class), eq(false));
    }
}