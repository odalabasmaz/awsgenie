package io.github.odalabasmaz.awsgenie.fetcher.cloudwatch;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.DescribeAlarmsRequest;
import com.amazonaws.services.cloudwatch.model.DescribeAlarmsResult;
import com.amazonaws.services.cloudwatch.model.MetricAlarm;
import io.github.odalabasmaz.awsgenie.fetcher.ResourceFetcherConfiguration;
import io.github.odalabasmaz.awsgenie.fetcher.credentials.AWSClientProvider;
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
        AWSClientProvider.class
})
@PowerMockIgnore({
        "javax.management.*", "javax.script.*"
})
public class CloudWatchResourceFetcherTest {
    private static final String TEST_REGION = "us-west-2";

    @Mock
    private ResourceFetcherConfiguration resourceFetcherConfiguration;

    @Mock
    private AmazonCloudWatch cloudWatchClient;

    @Mock
    private AWSClientProvider awsClientProvider;

    private CloudWatchResourceFetcher cloudwatchResourceFetcher;

    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(AWSClientProvider.class);
        when(AWSClientProvider.getInstance(resourceFetcherConfiguration))
                .thenReturn(awsClientProvider);
        when(awsClientProvider.getAmazonCloudWatch())
                .thenReturn(cloudWatchClient);

        this.cloudwatchResourceFetcher = new CloudWatchResourceFetcher(resourceFetcherConfiguration);

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
        cloudwatchResourceFetcher.listResources(TEST_REGION, actualAlarms::addAll);

        verify(cloudWatchClient, times(2)).describeAlarms(org.mockito.Mockito.any(DescribeAlarmsRequest.class));
        assertThat(actualAlarms.size(), is(equalTo(4)));
        assertThat(actualAlarms, hasItem("alarm1"));
        assertThat(actualAlarms, hasItem("alarm2"));
        assertThat(actualAlarms, hasItem("alarm3"));
        assertThat(actualAlarms, hasItem("alarm4"));
    }

    @Test
    public void fetchResources() {
        List<CloudWatchResource> actualAlarms = cloudwatchResourceFetcher.fetchResources(TEST_REGION, null, null);

        verify(cloudWatchClient, times(2)).describeAlarms(org.mockito.Mockito.any(DescribeAlarmsRequest.class));
        assertThat(actualAlarms.size(), is(equalTo(4)));
        assertThat(actualAlarms, hasItem(new CloudWatchResource().setResourceName("alarm1")));
        assertThat(actualAlarms, hasItem(new CloudWatchResource().setResourceName("alarm2")));
        assertThat(actualAlarms, hasItem(new CloudWatchResource().setResourceName("alarm3")));
        assertThat(actualAlarms, hasItem(new CloudWatchResource().setResourceName("alarm4")));
    }
}