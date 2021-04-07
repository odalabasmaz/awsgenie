package com.atlassian.awstool.terminate.cloudwatch;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.DescribeAlarmsRequest;
import com.amazonaws.services.cloudwatch.model.DescribeAlarmsResult;
import com.amazonaws.services.cloudwatch.model.MetricAlarm;
import com.atlassian.awstool.terminate.AWSResource;
import com.atlassian.awstool.terminate.FetchResources;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author Orhun Dalabasmaz
 * @version 10.03.2021
 */

public class FetchCloudwatchResources implements FetchResources {
    private static final Logger LOGGER = LogManager.getLogger(FetchCloudwatchResources.class);

    private final AWSCredentialsProvider credentialsProvider;

    public FetchCloudwatchResources(AWSCredentialsProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
    }

    @Override
    public List<? extends AWSResource> fetchResources(String region, List<String> resources, List<String> details) {
        AmazonCloudWatch cloudWatchClient = AmazonCloudWatchClient
                .builder()
                .withRegion(region)
                .withCredentials(credentialsProvider)
                .build();

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
    public void listResources(String region, Consumer<List<? extends AWSResource>> consumer) {

    }
}
