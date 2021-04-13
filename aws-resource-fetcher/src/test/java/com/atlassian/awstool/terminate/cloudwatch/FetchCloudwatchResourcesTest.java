package com.atlassian.awstool.terminate.cloudwatch;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.DescribeAlarmsRequest;
import com.amazonaws.services.cloudwatch.model.DescribeAlarmsResult;
import com.amazonaws.services.cloudwatch.model.MetricAlarm;
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
 * @version 12.04.2021
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        AwsClientProvider.class
})
@PowerMockIgnore({
        "javax.management.*", "javax.script.*"
})
public class FetchCloudwatchResourcesTest {
    private static final String TEST_REGION = "us-west-2";

    @Mock
    private FetcherConfiguration fetcherConfiguration;

    @Mock
    private AmazonCloudWatch cloudWatchClient;

    @Mock
    private AwsClientProvider awsClientProvider;

    private FetchCloudwatchResources fetchCloudwatchResources;

    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(AwsClientProvider.class);
        when(AwsClientProvider.getInstance(fetcherConfiguration))
                .thenReturn(awsClientProvider);
        when(awsClientProvider.getAmazonCloudWatch())
                .thenReturn(cloudWatchClient);

        this.fetchCloudwatchResources = new FetchCloudwatchResources(fetcherConfiguration);

        when(cloudWatchClient.describeAlarms(new DescribeAlarmsRequest()))
                .thenReturn(new DescribeAlarmsResult()
                        .withMetricAlarms(new ArrayList<MetricAlarm>() {{
                            add(new MetricAlarm().withAlarmName("alarm1"));
                            add(new MetricAlarm().withAlarmName("alarm2"));
                        }})
                        .withNextToken("nextToken"));
        when(cloudWatchClient.describeAlarms(new DescribeAlarmsRequest().withNextToken("nextToken")))
                .thenReturn(new DescribeAlarmsResult().withMetricAlarms(new ArrayList<MetricAlarm>() {{
                    add(new MetricAlarm().withAlarmName("alarm3"));
                    add(new MetricAlarm().withAlarmName("alarm4"));
                }}));
    }

    @Test
    public void listResources() {
        List<String> actualAlarms = new ArrayList<>();
        fetchCloudwatchResources.listResources(TEST_REGION, actualAlarms::addAll);

        verify(cloudWatchClient, times(2)).describeAlarms(org.mockito.Mockito.any(DescribeAlarmsRequest.class));
        assertThat(actualAlarms.size(), is(equalTo(4)));
        assertThat(actualAlarms, hasItem("alarm1"));
        assertThat(actualAlarms, hasItem("alarm2"));
        assertThat(actualAlarms, hasItem("alarm3"));
        assertThat(actualAlarms, hasItem("alarm4"));
    }

    @Test
    public void fetchResources() {
        List<CloudwatchResource> actualAlarms = fetchCloudwatchResources.fetchResources(TEST_REGION, null, null);

        verify(cloudWatchClient, times(2)).describeAlarms(org.mockito.Mockito.any(DescribeAlarmsRequest.class));
        assertThat(actualAlarms.size(), is(equalTo(4)));
        assertThat(actualAlarms, hasItem(new CloudwatchResource().setResourceName("alarm1")));
        assertThat(actualAlarms, hasItem(new CloudwatchResource().setResourceName("alarm2")));
        assertThat(actualAlarms, hasItem(new CloudwatchResource().setResourceName("alarm3")));
        assertThat(actualAlarms, hasItem(new CloudwatchResource().setResourceName("alarm4")));
    }
}