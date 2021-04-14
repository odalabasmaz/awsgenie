package com.atlassian.comparator.analyser;

import com.atlassian.comparator.ResourceComparator;
import io.awsgenie.fetcher.ResourceFetcher;
import io.awsgenie.fetcher.kinesis.KinesisResource;

import java.util.LinkedHashSet;
import java.util.stream.Collectors;

public class KinesisAnalyzer extends BaseResourceAnalyzer<KinesisResource> {
    public KinesisAnalyzer(ResourceComparator comparator, ResourceFetcher sourceResourceFetcher, ResourceFetcher targetResourceFetcher, long sleepBetweenIterations) {
        super(comparator, sourceResourceFetcher, targetResourceFetcher, sleepBetweenIterations);
    }

    @Override
    protected void replace(KinesisResource awsResource) {
        awsResource.setResourceName(replaceKeyword(awsResource.getResourceName()));
        awsResource.setCloudwatchAlarmList(new LinkedHashSet<>(awsResource.getCloudwatchAlarmList().stream().map(this::replaceKeyword).collect(Collectors.toSet())));
    }
}
