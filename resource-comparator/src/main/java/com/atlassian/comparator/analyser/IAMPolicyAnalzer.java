package com.atlassian.comparator.analyser;

import com.atlassian.comparator.ResourceComparator;
import io.awsgenie.fetcher.ResourceFetcher;
import io.awsgenie.fetcher.iam.IAMPolicyResource;

public class IAMPolicyAnalzer extends BaseResourceAnalyzer<IAMPolicyResource> {
    public IAMPolicyAnalzer(ResourceComparator comparator, ResourceFetcher sourceResourceFetcher, ResourceFetcher targetResourceFetcher, long sleepBetweenIterations) {
        super(comparator, sourceResourceFetcher, targetResourceFetcher, sleepBetweenIterations);
    }

    @Override
    protected void replace(IAMPolicyResource awsResource) {
        awsResource.setResourceName(replaceKeyword(awsResource.getResourceName()));
    }
}
