package com.atlassian.comparator.analyser;

import com.atlassian.comparator.ResourceComparator;
import io.awsgenie.fetcher.ResourceFetcher;
import io.awsgenie.fetcher.sns.SNSResource;

import java.util.LinkedHashSet;
import java.util.stream.Collectors;

public class SNSResourceAnalyzer  extends BaseResourceAnalyzer<SNSResource> {

    public SNSResourceAnalyzer(ResourceComparator comparator, ResourceFetcher sourceResourceFetcher, ResourceFetcher targetResourceFetcher, long sleepBetweenIterations) {
        super(comparator, sourceResourceFetcher, targetResourceFetcher, sleepBetweenIterations);
    }

    @Override
    protected void replace(SNSResource snsResource) {
        snsResource.setResourceName(replaceKeyword(snsResource.getResourceName()));
        snsResource.setCloudwatchAlarms(new LinkedHashSet<>(snsResource.getCloudwatchAlarms().stream().map(this::replaceKeyword).collect(Collectors.toSet())));

    }


}
