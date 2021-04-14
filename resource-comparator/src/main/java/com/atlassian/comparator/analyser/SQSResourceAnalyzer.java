package com.atlassian.comparator.analyser;

import com.atlassian.comparator.ResourceComparator;
import io.awsgenie.fetcher.ResourceFetcher;
import io.awsgenie.fetcher.sqs.SQSResource;

import java.util.*;
import java.util.stream.Collectors;

public class SQSResourceAnalyzer extends BaseResourceAnalyzer<SQSResource> {

    private final List<String> SQS_ATTRIBUTES = Arrays.asList(
            "VisibilityTimeout",
            "DelaySeconds",
            "MessageRetentionPeriod"
    );

    public SQSResourceAnalyzer(ResourceComparator comparator, ResourceFetcher sourceResourceFetcher, ResourceFetcher targetResourceFetcher, long sleepBetweenIterations) {
        super(comparator, sourceResourceFetcher, targetResourceFetcher, sleepBetweenIterations);
    }

    protected void replace(SQSResource awsResource) {
        awsResource.setResourceName(replaceKeyword(awsResource.getResourceName()));
        awsResource.setCloudwatchAlarms(new LinkedHashSet<>(awsResource.getCloudwatchAlarms().stream().map(this::replaceKeyword).collect(Collectors.toSet())));
        awsResource.setLambdaTriggers(new LinkedHashSet<>(awsResource.getLambdaTriggers().stream().map(this::replaceKeyword).collect(Collectors.toSet())));
        awsResource.setSnsSubscriptions(new LinkedHashSet<>(awsResource.getSnsSubscriptions().stream().map(this::replaceKeyword).collect(Collectors.toSet())));
    }
}