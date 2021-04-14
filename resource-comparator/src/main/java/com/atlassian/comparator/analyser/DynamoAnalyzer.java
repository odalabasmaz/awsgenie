package com.atlassian.comparator.analyser;

import com.atlassian.comparator.ResourceComparator;
import io.awsgenie.fetcher.ResourceFetcher;
import io.awsgenie.fetcher.dynamodb.DynamoDBResource;

import java.util.LinkedHashSet;
import java.util.stream.Collectors;

public class DynamoAnalyzer extends BaseResourceAnalyzer<DynamoDBResource> {

    public DynamoAnalyzer(ResourceComparator comparator, ResourceFetcher sourceResourceFetcher, ResourceFetcher targetResourceFetcher, long sleepBetweenIterations) {
        super(comparator, sourceResourceFetcher, targetResourceFetcher, sleepBetweenIterations);
    }

    @Override
    protected void replace(DynamoDBResource awsResource) {
        awsResource.setResourceName(replaceKeyword(awsResource.getResourceName()));
        awsResource.setCloudwatchAlarmList(new LinkedHashSet<>(awsResource.getCloudwatchAlarmList().stream().map(this::replaceKeyword).collect(Collectors.toSet())));
    }
}
