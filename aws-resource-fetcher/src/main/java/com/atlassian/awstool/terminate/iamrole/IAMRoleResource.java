package com.atlassian.awstool.terminate.iamrole;

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

    public IAMRoleResource addInlinePolicies(Set<IamEntity> inlinePolicies) {
        this.inlinePolicies.addAll(inlinePolicies);
        return this;
    }

    public LinkedHashSet<IamEntity> getInstanceProfiles() {
        return instanceProfiles;
    }

    public IAMRoleResource addInstanceProfiles(Set<IamEntity> instanceProfiles) {
        this.instanceProfiles.addAll(instanceProfiles);
        return this;
    }

    public LinkedHashSet<IamEntity> getAttachedPolicies() {
        return attachedPolicies;
    }

    public IAMRoleResource addAttachedPolicies(Set<IamEntity> attachedPolicies) {
        this.attachedPolicies.addAll(attachedPolicies);
        return this;
    }
}
