package com.atlassian.awsgenie.fetcher.cloudwatch;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.DescribeAlarmsRequest;
import com.amazonaws.services.cloudwatch.model.DescribeAlarmsResult;
import com.amazonaws.services.cloudwatch.model.MetricAlarm;
import com.atlassian.awsgenie.fetcher.ResourceFetcher;
import com.atlassian.awsgenie.fetcher.ResourceFetcherConfiguration;
import com.atlassian.awsgenie.fetcher.ResourceFetcherWithProvider;
import com.atlassian.awsgenie.fetcher.credentials.AWSClientProvider;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author Orhun Dalabasmaz
 * @version 10.03.2021
 */

public class CloudWatchResourceFetcher extends ResourceFetcherWithProvider implements ResourceFetcher<CloudWatchResource> {


    public CloudWatchResourceFetcher(ResourceFetcherConfiguration configuration) {
        super(configuration);
    }


    @Override
    public List<CloudWatchResource> fetchResources(String region, List<String> resources, List<String> details) {
        AmazonCloudWatch cloudWatchClient = AWSClientProvider.getInstance(getConfiguration()).getAmazonCloudWatch();

        // Resources to be removed
        LinkedHashSet<String> cloudwatchAlarmsToDelete = new LinkedHashSet<>();

        // gather each alarm
        String nextToken = null;
        do {
            DescribeAlarmsResult result = cloudWatchClient.describeAlarms(
                    new DescribeAlarmsRequest().withAlarmNames(resources).withNextToken(nextToken));
            result.getMetricAlarms()
                    .stream()
                    .map(MetricAlarm::getAlarmName)
                    .forEach(cloudwatchAlarmsToDelete::add);
            nextToken = result.getNextToken();
        } while (nextToken != null);

        List<CloudWatchResource> cloudWatchResourceList = new ArrayList<>();
        for (String cloudwatchAlarmToDelete : cloudwatchAlarmsToDelete) {
            cloudWatchResourceList.add(new CloudWatchResource().setResourceName(cloudwatchAlarmToDelete));
        }
        return cloudWatchResourceList;
    }


    @Override
    public void listResources(String region, Consumer<List<String>> consumer) {
        AmazonCloudWatch cloudWatchClient = AWSClientProvider.getInstance(getConfiguration()).getAmazonCloudWatch();
        consume((nextMarker) -> {
            DescribeAlarmsResult describeAlarmsResult = cloudWatchClient.describeAlarms(new DescribeAlarmsRequest().withNextToken(nextMarker));
            List<String> alarmList = describeAlarmsResult.getMetricAlarms().stream().map(MetricAlarm::getAlarmName).collect(Collectors.toList());
            consumer.accept(alarmList);
            return describeAlarmsResult.getNextToken();
        });
    }
}
