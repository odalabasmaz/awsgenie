package com.atlassian.awstool.terminate.iamPolicy;

import com.atlassian.awstool.terminate.AWSResource;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Objects;

public class IAMPolicyResource extends AWSResource {
    private String resourceName;

    @Override
    public String getResourceName() {
        return resourceName;
    }

    public IAMPolicyResource setResourceName(String resourceName) {
        this.resourceName = resourceName;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IAMPolicyResource that = (IAMPolicyResource) o;
        return Objects.equals(resourceName, that.resourceName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceName);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("resourceName", resourceName)
                .toString();
    }
}
