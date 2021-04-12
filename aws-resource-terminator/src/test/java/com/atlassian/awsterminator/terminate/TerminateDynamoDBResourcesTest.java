package com.atlassian.awsterminator.terminate;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.DeleteAlarmsRequest;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.atlassian.awsterminator.interceptor.AfterTerminateInterceptor;
import com.atlassian.awsterminator.interceptor.BeforeTerminateInterceptor;
import com.atlassian.awsterminator.interceptor.InterceptorRegistry;
import com.atlassian.awstool.terminate.AWSResource;
import com.atlassian.awstool.terminate.FetchResourceFactory;
import com.atlassian.awstool.terminate.FetchResources;
import com.atlassian.awstool.terminate.Service;
import com.atlassian.awstool.terminate.dynamodb.DynamodbResource;
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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;


/**
 * @author Celal Emre CICEK
 * @version 9.04.2021
 */

@RunWith(MockitoJUnitRunner.class)
public class TerminateDynamoDBResourcesTest {
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

    private static final List<AWSResource> TEST_FETCHED_RESOURCES = new ArrayList<AWSResource>() {{
        add(new DynamodbResource()
                .setResourceName("table1")
                .setCloudwatchAlarmList(new LinkedHashSet<String>() {{
                    add("table1 Read");
                    add("table1 Write");
                }}));
        //.setTotalUsage(0.0));
        add(new DynamodbResource()
                .setResourceName("table2"));
        //.setTotalUsage(1.0));
        add(new DynamodbResource()
                .setResourceName("table3"));
        //.setTotalUsage(0.0));
    }};

    @Mock
    private AmazonCloudWatch cloudWatchClient;

    @Mock
    private AmazonDynamoDB dynamoDBClient;

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

    private TerminateDynamoDBResources terminateDynamoDBResources;

    @Before
    public void setUp() throws Exception {
        InterceptorRegistry.getBeforeTerminateInterceptors().clear();
        InterceptorRegistry.getAfterTerminateInterceptors().clear();
        this.terminateDynamoDBResources = new TerminateDynamoDBResources(credentialsProvider);
        terminateDynamoDBResources.setCloudWatchClient(cloudWatchClient);
        terminateDynamoDBResources.setDynamoDBClient(dynamoDBClient);
        terminateDynamoDBResources.setFetchResourceFactory(fetchResourceFactory);

        when(fetchResourceFactory.getFetcher(service, credentialsProvider))
                .thenReturn(fetchResources);
        doReturn(TEST_FETCHED_RESOURCES)
                .when(fetchResources).fetchResources(eq(TEST_REGION), eq(TEST_RESOURCES), org.mockito.Mockito.any(List.class));
        doReturn(0.0)
                .when(fetchResources).getUsage(eq(TEST_REGION), eq(TABLE_1));
        doReturn(1.0)
                .when(fetchResources).getUsage(eq(TEST_REGION), eq(TABLE_2));
        doReturn(0.0)
                .when(fetchResources).getUsage(eq(TEST_REGION), eq(TABLE_3));
    }

    @Test
    public void terminateResourcesWithApply() throws Exception {
        terminateDynamoDBResources.terminateResource(TEST_REGION, service, TEST_RESOURCES, TEST_TICKET, true);

        verify(dynamoDBClient).deleteTable("table1");
        verify(dynamoDBClient).deleteTable("table3");
        verifyNoMoreInteractions(dynamoDBClient);

        ArgumentCaptor<DeleteAlarmsRequest> cwCaptor = ArgumentCaptor.forClass(DeleteAlarmsRequest.class);
        verify(cloudWatchClient).deleteAlarms(cwCaptor.capture());
        verifyNoMoreInteractions(cloudWatchClient);
        DeleteAlarmsRequest actualCWRequest = cwCaptor.getValue();
        assertThat(actualCWRequest.getAlarmNames().size(), is(equalTo(2)));
        assertThat(actualCWRequest.getAlarmNames(), hasItem("table1 Read"));
        assertThat(actualCWRequest.getAlarmNames(), hasItem("table1 Write"));
    }

    @Test
    public void terminateResourcesWithoutApply() throws Exception {
        terminateDynamoDBResources.terminateResource(TEST_REGION, service, TEST_RESOURCES, TEST_TICKET, false);
        verifyZeroInteractions(cloudWatchClient);
        verifyZeroInteractions(dynamoDBClient);
    }

    @Test
    public void interceptorsAreCalled() throws Exception {
        InterceptorRegistry.addInterceptor(beforeTerminateInterceptor);
        InterceptorRegistry.addInterceptor(afterTerminateInterceptor);
        terminateDynamoDBResources.terminateResource(TEST_REGION, service, TEST_RESOURCES, TEST_TICKET, false);
        verify(beforeTerminateInterceptor).intercept(eq(service), eq(TEST_FETCHED_RESOURCES), any(String.class), eq(false));
        verify(afterTerminateInterceptor).intercept(eq(service), eq(TEST_FETCHED_RESOURCES), any(String.class), eq(false));
    }
}