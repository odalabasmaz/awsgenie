package com.atlassian.comparator.analyser;

import com.atlassian.comparator.ResourceComparator;
import io.awsgenie.fetcher.ResourceFetcher;
import io.awsgenie.fetcher.lambda.LambdaResource;

import java.util.LinkedHashSet;
import java.util.stream.Collectors;

public class LambdaAnalyzer extends BaseResourceAnalyzer<LambdaResource> {


    public LambdaAnalyzer(ResourceComparator comparator, ResourceFetcher sourceResourceFetcher, ResourceFetcher targetResourceFetcher, long sleepBetweenIterations) {
        super(comparator, sourceResourceFetcher, targetResourceFetcher, sleepBetweenIterations);
    }

    @Override
    protected void replace(LambdaResource awsResource) {
        awsResource.setResourceName(replaceKeyword(awsResource.getResourceName()));
        awsResource.setCloudwatchAlarms(new LinkedHashSet<>(awsResource.getCloudwatchAlarms().stream().map(this::replaceKeyword).collect(Collectors.toSet())));
        awsResource.setCloudwatchRules(new LinkedHashSet<>(awsResource.getCloudwatchRules().stream().map(this::replaceKeyword).collect(Collectors.toSet())));
        awsResource.setCloudwatchRuleTargets(new LinkedHashSet<>(awsResource.getCloudwatchRuleTargets().stream().map(this::replaceKeyword).collect(Collectors.toSet())));
        awsResource.setEventSourceMappings(new LinkedHashSet<>(awsResource.getEventSourceMappings().stream().map(this::replaceKeyword).collect(Collectors.toSet())));
        awsResource.setSnsTriggers(new LinkedHashSet<>(awsResource.getSnsTriggers().stream().map(this::replaceKeyword).collect(Collectors.toSet())));
    }
}
