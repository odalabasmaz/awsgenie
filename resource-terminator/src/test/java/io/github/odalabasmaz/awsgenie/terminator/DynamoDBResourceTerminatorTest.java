package io.github.odalabasmaz.awsgenie.terminator;

import com.amazonaws.services.cloudwatch.model.DeleteAlarmsRequest;
import io.github.odalabasmaz.awsgenie.fetcher.Resource;
import io.github.odalabasmaz.awsgenie.fetcher.Service;
import io.github.odalabasmaz.awsgenie.fetcher.dynamodb.DynamoDBResource;
import io.github.odalabasmaz.awsgenie.terminator.interceptor.AfterTerminateInterceptor;
import io.github.odalabasmaz.awsgenie.terminator.interceptor.BeforeTerminateInterceptor;
import io.github.odalabasmaz.awsgenie.terminator.interceptor.InterceptorRegistry;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;


/**
 * @author Celal Emre CICEK
 * @version 9.04.2021
 */

public class DynamoDBResourceTerminatorTest extends TerminatorTest {
    private static final String TEST_REGION = "us-west-2";
    private static final String TEST_TICKET = "TEST-TICKET";
    public static final String TABLE_1 = "table1";
    public static final String TABLE_2 = "table2";
    public static final String TABLE_3 = "table3";
    private final Service service = Service.DYNAMODB;

    private static final List<String> TEST_RESOURCES = new ArrayList<String>() {{
        add(TABLE_1);
        add(TABLE_2);
        add(TABLE_3);
    }};

    private static final List<Resource> TEST_FETCHED_RESOURCES = new ArrayList<Resource>() {{
        add(new DynamoDBResource()
                .setResourceName(TABLE_1)
                .setCloudwatchAlarmList(new LinkedHashSet<String>() {{
                    add("table1 Read");
                    add("table1 Write");
                }}));
        //.setTotalUsage(0.0));
        add(new DynamoDBResource()
                .setResourceName(TABLE_2));
        //.setTotalUsage(1.0));
        add(new DynamoDBResource()
                .setResourceName(TABLE_3));
        //.setTotalUsage(0.0));
    }};


    @Mock
    private BeforeTerminateInterceptor beforeTerminateInterceptor;

    @Mock
    private AfterTerminateInterceptor afterTerminateInterceptor;

    private DynamoDBResourceTerminator dynamoDBResourceTerminator;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        this.dynamoDBResourceTerminator = new DynamoDBResourceTerminator(TerminatorHelper.getRegion1Account1Configuration());
        dynamoDBResourceTerminator.setFetchResourceFactory(getFetchResourceFactory());
        doReturn(TEST_FETCHED_RESOURCES)
                .when(getFetchResources()).fetchResources(eq(TEST_REGION), eq(TEST_RESOURCES), org.mockito.Mockito.any(List.class));
        doReturn(0.0)
                .when(getFetchResources()).getUsage(eq(TEST_REGION), eq(TABLE_1), eq(7));
        doReturn(1.0)
                .when(getFetchResources()).getUsage(eq(TEST_REGION), eq(TABLE_2), eq(7));
        doReturn(0.0)
                .when(getFetchResources()).getUsage(eq(TEST_REGION), eq(TABLE_3), eq(7));
    }

    @Test
    public void terminateResourcesWithApply() throws Exception {
        dynamoDBResourceTerminator.terminateResource(TEST_REGION, service, TEST_RESOURCES, TEST_TICKET, true);

        verify(getAmazonDynamoDB()).deleteTable(TABLE_1);
        verify(getAmazonDynamoDB()).deleteTable(TABLE_3);
        verifyNoMoreInteractions(getAmazonDynamoDB());

        ArgumentCaptor<DeleteAlarmsRequest> cwCaptor = ArgumentCaptor.forClass(DeleteAlarmsRequest.class);
        verify(getCloudWatchClient()).deleteAlarms(cwCaptor.capture());
        verifyNoMoreInteractions(getCloudWatchClient());
        DeleteAlarmsRequest actualCWRequest = cwCaptor.getValue();
        assertThat(actualCWRequest.getAlarmNames().size(), is(equalTo(2)));
        assertThat(actualCWRequest.getAlarmNames(), hasItem("table1 Read"));
        assertThat(actualCWRequest.getAlarmNames(), hasItem("table1 Write"));
    }

    @Test
    public void terminateResourcesWithoutApply() throws Exception {
        dynamoDBResourceTerminator.terminateResource(TEST_REGION, service, TEST_RESOURCES, TEST_TICKET, false);
        verifyZeroInteractions(getCloudWatchClient());
        verifyZeroInteractions(getAmazonDynamoDB());
    }

    @Test
    public void interceptorsAreCalled() throws Exception {
        InterceptorRegistry.addInterceptor(beforeTerminateInterceptor);
        InterceptorRegistry.addInterceptor(afterTerminateInterceptor);
        dynamoDBResourceTerminator.terminateResource(TEST_REGION, service, TEST_RESOURCES, TEST_TICKET, false);
        verify(beforeTerminateInterceptor).intercept(eq(service), eq(TEST_FETCHED_RESOURCES), any(String.class), eq(false));
        verify(afterTerminateInterceptor).intercept(eq(service), eq(TEST_FETCHED_RESOURCES), any(String.class), eq(false));
    }
}