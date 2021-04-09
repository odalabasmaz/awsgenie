package com.atlassian.awsterminator.terminate;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.DeleteAlarmsRequest;
import com.atlassian.awstool.terminate.AWSResource;
import com.atlassian.awstool.terminate.FetchResourceFactory;
import com.atlassian.awstool.terminate.FetchResources;
import com.atlassian.awstool.terminate.cloudwatch.CloudwatchResource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

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

@RunWith(MockitoJUnitRunner.class)
public class TerminateCloudwatchResourcesTest {
    private static final String TEST_REGION = "us-west-2";
    private static final String TEST_TICKET = "TEST-TICKET";

    private static final List<String> TEST_RESOURCES = new ArrayList<String>() {{
        add("Alarm res1");
        add("Alarm res2");
        add("Alarm res3");
    }};

    private static final List<AWSResource> TEST_FETCHED_RESOURCES = new ArrayList<AWSResource>() {{
        add(new CloudwatchResource().setResourceName("Alarm res1"));
        add(new CloudwatchResource().setResourceName("Alarm res2"));
    }};

    @Mock
    private AmazonCloudWatch cloudWatchClient;

    @Mock
    private AWSCredentialsProvider credentialsProvider;

    @Mock
    private FetchResourceFactory fetchResourceFactory;

    @Mock
    private FetchResources fetchResources;

    private TerminateCloudwatchResources terminateCloudwatchResources;

    @Before
    public void setUp() throws Exception {
        this.terminateCloudwatchResources = new TerminateCloudwatchResources(credentialsProvider);
        terminateCloudwatchResources.setCloudWatchClient(cloudWatchClient);
        terminateCloudwatchResources.setFetchResourceFactory(fetchResourceFactory);

        when(fetchResourceFactory.getFetcher("cloudwatch", credentialsProvider))
                .thenReturn(fetchResources);
        doReturn(TEST_FETCHED_RESOURCES)
                .when(fetchResources).fetchResources(TEST_REGION, TEST_RESOURCES, null);
    }

    @Test
    public void terminateResourcesWithApply() throws Exception {
        terminateCloudwatchResources.terminateResource(TEST_REGION, "cloudwatch", TEST_RESOURCES, TEST_TICKET, true);

        ArgumentCaptor<DeleteAlarmsRequest> captor = ArgumentCaptor.forClass(DeleteAlarmsRequest.class);
        verify(cloudWatchClient).deleteAlarms(captor.capture());
        DeleteAlarmsRequest actualRequest = captor.getValue();
        assertThat(actualRequest.getAlarmNames(), hasItem("Alarm res1"));
        assertThat(actualRequest.getAlarmNames(), hasItem("Alarm res2"));
        assertThat(actualRequest.getAlarmNames(), not(hasItem("Alarm res3")));
    }

    @Test
    public void terminateResourcesWithoutApply() throws Exception {
        terminateCloudwatchResources.terminateResource(TEST_REGION, "cloudwatch", TEST_RESOURCES, TEST_TICKET, false);
        verifyZeroInteractions(cloudWatchClient);
    }
}