package com.atlassian.comparator.analyser;

import com.atlassian.comparator.ResourceComparator;
import io.awsgenie.fetcher.ResourceFetcher;
import io.awsgenie.fetcher.iam.IAMRoleResource;

import java.util.LinkedHashSet;
import java.util.stream.Collectors;

public class IamRoleAnalyzer extends BaseResourceAnalyzer<IAMRoleResource> {

    public IamRoleAnalyzer(ResourceComparator comparator, ResourceFetcher sourceResourceFetcher, ResourceFetcher targetResourceFetcher, long sleepBetweenIterations) {
        super(comparator, sourceResourceFetcher, targetResourceFetcher, sleepBetweenIterations);
    }

    @Override
    protected void replace(IAMRoleResource awsResource) {
        awsResource.setResourceName(replaceKeyword(awsResource.getResourceName()));
        awsResource.setAttachedPolicies(new LinkedHashSet<>(awsResource.getAttachedPolicies().stream().peek(it -> {
            it.setRoleName(replaceKeyword(it.getRoleName()));
            it.setEntityName(replaceKeyword(it.getEntityName()));
        }).collect(Collectors.toSet())));
        awsResource.setInlinePolicies(new LinkedHashSet<>(awsResource.getInlinePolicies().stream().peek(it -> {
            it.setRoleName(replaceKeyword(it.getRoleName()));
            it.setEntityName(replaceKeyword(it.getEntityName()));
        }).collect(Collectors.toSet())));
        awsResource.setInstanceProfiles(new LinkedHashSet<>(awsResource.getInstanceProfiles().stream().peek(it -> {
            it.setRoleName(replaceKeyword(it.getRoleName()));
            it.setEntityName(replaceKeyword(it.getEntityName()));
        }).collect(Collectors.toSet())));
    }
}
