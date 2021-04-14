package com.atlassian.comparator.analyser;

import com.atlassian.comparator.ResourceComparator;
import io.awsgenie.fetcher.ResourceFetcher;
import io.awsgenie.fetcher.cloudwatch.CloudWatchResource;

public class CloudWatchAnalyzer extends BaseResourceAnalyzer<CloudWatchResource> {

    public CloudWatchAnalyzer(ResourceComparator comparator, ResourceFetcher sourceResourceFetcher, ResourceFetcher targetResourceFetcher, long sleepBetweenIterations) {
        super(comparator, sourceResourceFetcher, targetResourceFetcher, sleepBetweenIterations);
    }

    @Override
    protected void replace(CloudWatchResource awsResource) {
        awsResource.setResourceName(replaceKeyword(awsResource.getResourceName()));
    }
}
