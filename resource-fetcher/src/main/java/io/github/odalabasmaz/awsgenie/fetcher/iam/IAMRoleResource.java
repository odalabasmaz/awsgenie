package io.github.odalabasmaz.awsgenie.fetcher.iam;

import io.github.odalabasmaz.awsgenie.fetcher.Resource;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public class IAMRoleResource extends Resource {
    private String resourceName;
    private LinkedHashSet<IAMEntity> inlinePolicies = new LinkedHashSet<>();
    private LinkedHashSet<IAMEntity> instanceProfiles = new LinkedHashSet<>();
    private LinkedHashSet<IAMEntity> attachedPolicies = new LinkedHashSet<>();

    @Override
    public String getResourceName() {
        return resourceName;
    }

    public IAMRoleResource setResourceName(String resourceName) {
        this.resourceName = resourceName;
        return this;
    }

    public LinkedHashSet<IAMEntity> getInlinePolicies() {
        return inlinePolicies;
    }

    public IAMRoleResource setInlinePolicies(Set<IAMEntity> inlinePolicies) {
        this.inlinePolicies = new LinkedHashSet<>(inlinePolicies);
        return this;
    }

    public void addInlinePolicies(Set<IAMEntity> inlinePolicies) {
        this.inlinePolicies.addAll(inlinePolicies);
    }

    public LinkedHashSet<IAMEntity> getInstanceProfiles() {
        return instanceProfiles;
    }

    public IAMRoleResource setInstanceProfiles(Set<IAMEntity> instanceProfiles) {
        this.instanceProfiles = new LinkedHashSet<>(instanceProfiles);
        return this;
    }

    public void addInstanceProfiles(Set<IAMEntity> instanceProfiles) {
        this.instanceProfiles.addAll(instanceProfiles);
    }

    public LinkedHashSet<IAMEntity> getAttachedPolicies() {
        return attachedPolicies;
    }

    public IAMRoleResource setAttachedPolicies(Set<IAMEntity> attachedPolicies) {
        this.attachedPolicies = new LinkedHashSet<>(attachedPolicies);
        return this;
    }

    public void addAttachedPolicies(Set<IAMEntity> attachedPolicies) {
        this.attachedPolicies.addAll(attachedPolicies);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IAMRoleResource that = (IAMRoleResource) o;
        return Objects.equals(resourceName, that.resourceName) && Objects.equals(inlinePolicies, that.inlinePolicies) && Objects.equals(instanceProfiles, that.instanceProfiles) && Objects.equals(attachedPolicies, that.attachedPolicies);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceName, inlinePolicies, instanceProfiles, attachedPolicies);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("resourceName", resourceName)
                .append("inlinePolicies", inlinePolicies)
                .append("instanceProfiles", instanceProfiles)
                .append("attachedPolicies", attachedPolicies)
                .toString();
    }
}
