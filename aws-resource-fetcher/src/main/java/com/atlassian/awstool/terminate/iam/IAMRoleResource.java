package com.atlassian.awstool.terminate.iam;

import com.atlassian.awstool.terminate.AWSResource;

import java.util.LinkedHashSet;
import java.util.Set;

public class IAMRoleResource extends AWSResource {
    private String resourceName;
    private LinkedHashSet<IamEntity> inlinePolicies = new LinkedHashSet<>();
    private LinkedHashSet<IamEntity> instanceProfiles = new LinkedHashSet<>();
    private LinkedHashSet<IamEntity> attachedPolicies = new LinkedHashSet<>();

    @Override
    public String getResourceName() {
        return resourceName;
    }

    public IAMRoleResource setResourceName(String resourceName) {
        this.resourceName = resourceName;
        return this;
    }

    public LinkedHashSet<IamEntity> getInlinePolicies() {
        return inlinePolicies;
    }

    public IAMRoleResource setInlinePolicies(Set<IamEntity> inlinePolicies) {
        this.inlinePolicies = new LinkedHashSet<>(inlinePolicies);
        return this;
    }

    public void addInlinePolicies(Set<IamEntity> inlinePolicies) {
        this.inlinePolicies.addAll(inlinePolicies);
    }

    public LinkedHashSet<IamEntity> getInstanceProfiles() {
        return instanceProfiles;
    }

    public IAMRoleResource setInstanceProfiles(Set<IamEntity> instanceProfiles) {
        this.instanceProfiles = new LinkedHashSet<>(instanceProfiles);
        return this;
    }

    public void addInstanceProfiles(Set<IamEntity> instanceProfiles) {
        this.instanceProfiles.addAll(instanceProfiles);
    }

    public LinkedHashSet<IamEntity> getAttachedPolicies() {
        return attachedPolicies;
    }

    public IAMRoleResource setAttachedPolicies(Set<IamEntity> attachedPolicies) {
        this.attachedPolicies = new LinkedHashSet<>(attachedPolicies);
        return this;
    }

    public void addAttachedPolicies(Set<IamEntity> attachedPolicies) {
        this.attachedPolicies.addAll(attachedPolicies);
    }
}
