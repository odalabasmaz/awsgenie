package io.awsgenie.fetcher.cloudwatch;

import io.awsgenie.fetcher.Resource;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Objects;

public class CloudWatchResource extends Resource {
    private String resourceName;

    @Override
    public String getResourceName() {
        return resourceName;
    }

    public CloudWatchResource setResourceName(String resourceName) {
        this.resourceName = resourceName;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CloudWatchResource that = (CloudWatchResource) o;
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
