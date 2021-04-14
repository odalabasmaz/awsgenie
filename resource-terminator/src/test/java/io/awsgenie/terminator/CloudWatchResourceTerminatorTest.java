package io.awsgenie.terminator;

import com.amazonaws.services.cloudwatch.model.DeleteAlarmsRequest;
import io.awsgenie.fetcher.Resource;
import io.awsgenie.fetcher.Service;
import io.awsgenie.fetcher.cloudwatch.CloudWatchResource;
import io.awsgenie.terminator.interceptor.AfterTerminateInterceptor;
import io.awsgenie.terminator.interceptor.BeforeTerminateInterceptor;
import io.awsgenie.terminator.interceptor.InterceptorRegistry;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;


/**
 * @author Celal Emre CICEK
 * @version 9.04.2021
 */

public class CloudWatchResourceTerminatorTest extends TerminatorTest {
    private static final String TEST_REGION = "us-west-2";
    private static final String TEST_TICKET = "TEST-TICKET";
    private final Service service = Service.CLOUDWATCH;

    private static final List<String> TEST_RESOURCES = new ArrayList<String>() {{
        add("Alarm res1");
        add("Alarm res2");
        add("Alarm res3");
    }};

    private static final List<Resource> TEST_FETCHED_RESOURCES = new ArrayList<Resource>() {{
        add(new CloudWatchResource().setResourceName("Alarm res1"));
        add(new CloudWatchResource().setResourceName("Alarm res2"));
    }};

    @Mock
    private BeforeTerminateInterceptor beforeTerminateInterceptor;
    @Mock
    private AfterTerminateInterceptor afterTerminateInterceptor;

    private CloudWatchResourceTerminator cloudwatchResourceTerminator;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        this.cloudwatchResourceTerminator = new CloudWatchResourceTerminator(TerminatorHelper.getRegion1Account1Configuration());
        cloudwatchResourceTerminator.setFetchResourceFactory(getFetchResourceFactory());
        doReturn(TEST_FETCHED_RESOURCES)
                .when(getFetchResources()).fetchResources(TEST_REGION, TEST_RESOURCES, null);
    }

    @Test
    public void terminateResourcesWithApply() throws Exception {
        cloudwatchResourceTerminator.terminateResource(TEST_REGION, service, TEST_RESOURCES, TEST_TICKET, true);

        ArgumentCaptor<DeleteAlarmsRequest> captor = ArgumentCaptor.forClass(DeleteAlarmsRequest.class);
        verify(getCloudWatchClient()).deleteAlarms(captor.capture());
        verifyNoMoreInteractions(getCloudWatchClient());
        DeleteAlarmsRequest actualRequest = captor.getValue();
        assertThat(actualRequest.getAlarmNames(), hasItem("Alarm res1"));
        assertThat(actualRequest.getAlarmNames(), hasItem("Alarm res2"));
        assertThat(actualRequest.getAlarmNames(), not(hasItem("Alarm res3")));
    }

    @Test
    public void terminateResourcesWithoutApply() throws Exception {
        cloudwatchResourceTerminator.terminateResource(TEST_REGION, service, TEST_RESOURCES, TEST_TICKET, false);
        verifyZeroInteractions(getCloudWatchClient());
    }

    @Test
    public void interceptorsAreCalled() throws Exception {
        InterceptorRegistry.addInterceptor(beforeTerminateInterceptor);
        InterceptorRegistry.addInterceptor(afterTerminateInterceptor);
        cloudwatchResourceTerminator.terminateResource(TEST_REGION, service, TEST_RESOURCES, TEST_TICKET, false);
        verify(beforeTerminateInterceptor).intercept(eq(service), eq(TEST_FETCHED_RESOURCES), any(String.class), eq(false));
        verify(afterTerminateInterceptor).intercept(eq(service), eq(TEST_FETCHED_RESOURCES), any(String.class), eq(false));
    }
}