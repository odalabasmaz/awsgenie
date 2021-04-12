package com.atlassian.awstool.terminate.cloudwatch;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.DescribeAlarmsRequest;
import com.amazonaws.services.cloudwatch.model.DescribeAlarmsResult;
import com.amazonaws.services.cloudwatch.model.MetricAlarm;
import com.atlassian.awstool.terminate.FetchResources;
import com.atlassian.awstool.terminate.FetchResourcesWithProvider;
import com.atlassian.awstool.terminate.FetcherConfiguration;
import credentials.AwsClientProvider;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author Orhun Dalabasmaz
 * @version 10.03.2021
 */

public class FetchCloudwatchResources extends FetchResourcesWithProvider implements FetchResources<CloudwatchResource> {


    public FetchCloudwatchResources(FetcherConfiguration configuration) {
        super(configuration);
    }


    @Override
    public List<CloudwatchResource> fetchResources(List<String> resources, List<String> details) {
        AmazonCloudWatch cloudWatchClient = AwsClientProvider.getInstance(getConfiguration()).getAmazonCloudWatch();

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

        List<CloudwatchResource> cloudwatchResourceList = new ArrayList<>();
        for (String cloudwatchAlarmToDelete : cloudwatchAlarmsToDelete) {
            cloudwatchResourceList.add(new CloudwatchResource().setResourceName(cloudwatchAlarmToDelete));
        }
        return cloudwatchResourceList;
    }


    @Override
    public void listResources(Consumer<List<String>> consumer) {
        AmazonCloudWatch cloudWatchClient = AwsClientProvider.getInstance(getConfiguration()).getAmazonCloudWatch();
        consume((nextMarker) -> {
            DescribeAlarmsResult describeAlarmsResult = cloudWatchClient.describeAlarms(new DescribeAlarmsRequest().withNextToken(nextMarker));
            List<String> alarmList = describeAlarmsResult.getMetricAlarms().stream().map(MetricAlarm::getAlarmName).collect(Collectors.toList());
            consumer.accept(alarmList);
            return describeAlarmsResult.getNextToken();
        });
    }
}
