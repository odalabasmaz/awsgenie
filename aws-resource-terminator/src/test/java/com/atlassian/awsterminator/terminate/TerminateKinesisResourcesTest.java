package com.atlassian.awsterminator.terminate;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.DeleteAlarmsRequest;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.atlassian.awstool.terminate.AWSResource;
import com.atlassian.awstool.terminate.FetchResourceFactory;
import com.atlassian.awstool.terminate.FetchResources;
import com.atlassian.awstool.terminate.kinesis.KinesisResource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

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

@RunWith(MockitoJUnitRunner.class)
public class TerminateKinesisResourcesTest {
    private static final String TEST_REGION = "us-west-2";
    private static final String TEST_TICKET = "TEST-TICKET";

    private static final List<String> TEST_RESOURCES = new ArrayList<String>() {{
        add("stream1");
        add("stream2");
        add("stream3");
        add("stream4");
    }};

    private static final List<AWSResource> TEST_FETCHED_RESOURCES = new ArrayList<AWSResource>() {{
        add(new KinesisResource()
                .setResourceName("stream1")
                .setTotalUsage(0.0)
                .setCloudwatchAlarmList(new LinkedHashSet<String>() {{
                    add("Kinesis stream stream1 is Read throttled.");
                    add("Kinesis stream stream1 is Write throttled.");
                }}));
        add(new KinesisResource()
                .setResourceName("stream2")
                .setTotalUsage(0.0)
                .setCloudwatchAlarmList(new LinkedHashSet<String>() {{
                    add("Kinesis stream stream2 is Read throttled.");
                }}));
        add(new KinesisResource()
                .setResourceName("stream3")
                .setTotalUsage(1.0)
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

    private TerminateKinesisResources terminateKinesisResources;

    @Before
    public void setUp() throws Exception {
        this.terminateKinesisResources = new TerminateKinesisResources(credentialsProvider);
        terminateKinesisResources.setCloudWatchClient(cloudWatchClient);
        terminateKinesisResources.setKinesisClient(kinesisClient);
        terminateKinesisResources.setFetchResourceFactory(fetchResourceFactory);

        when(fetchResourceFactory.getFetcher("kinesis", credentialsProvider))
                .thenReturn(fetchResources);
        doReturn(TEST_FETCHED_RESOURCES)
                .when(fetchResources).fetchResources(eq(TEST_REGION), eq(TEST_RESOURCES), org.mockito.Mockito.any(List.class));
    }

    @Test
    public void terminateResourcesWithApply() throws Exception {
        terminateKinesisResources.terminateResource(TEST_REGION, "kinesis", TEST_RESOURCES, TEST_TICKET, true);

        verify(kinesisClient).deleteStream("stream1");
        verify(kinesisClient).deleteStream("stream2");
        verifyNoMoreInteractions(kinesisClient);

        ArgumentCaptor<DeleteAlarmsRequest> captor = ArgumentCaptor.forClass(DeleteAlarmsRequest.class);
        verify(cloudWatchClient).deleteAlarms(captor.capture());
        DeleteAlarmsRequest actualRequest = captor.getValue();
        assertThat(actualRequest.getAlarmNames(), hasItem("Kinesis stream stream1 is Read throttled."));
        assertThat(actualRequest.getAlarmNames(), hasItem("Kinesis stream stream1 is Write throttled."));
        assertThat(actualRequest.getAlarmNames(), hasItem("Kinesis stream stream2 is Read throttled."));
        assertThat(actualRequest.getAlarmNames(), not(hasItem("Kinesis stream stream3 is Read throttled.")));
        assertThat(actualRequest.getAlarmNames(), not(hasItem("Kinesis stream stream3 is Write throttled.")));
    }

    @Test
    public void terminateResourcesWithoutApply() throws Exception {
        terminateKinesisResources.terminateResource(TEST_REGION, "kinesis", TEST_RESOURCES, TEST_TICKET, false);
        verifyZeroInteractions(kinesisClient);
        verifyZeroInteractions(cloudWatchClient);
    }
}