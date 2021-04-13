package io.awsgenie.fetcher.sns;

import io.awsgenie.fetcher.Resource;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.LinkedHashSet;
import java.util.Objects;

public class SNSResource extends Resource {
    private String resourceName;
    private LinkedHashSet<String> cloudwatchAlarms = new LinkedHashSet<>();

    @Override
    public String getResourceName() {
        return resourceName;
    }

    @Override
    public Object getResourceObject() {
        return null;
    }

    public SNSResource setResourceName(String resourceName) {
        this.resourceName = resourceName;
        return this;
    }

    public LinkedHashSet<String> getCloudwatchAlarms() {
        return cloudwatchAlarms;
    }

    public SNSResource setCloudwatchAlarms(LinkedHashSet<String> cloudwatchAlarms) {
        this.cloudwatchAlarms = cloudwatchAlarms;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SNSResource that = (SNSResource) o;
        return Objects.equals(resourceName, that.resourceName) && Objects.equals(cloudwatchAlarms, that.cloudwatchAlarms);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceName, cloudwatchAlarms);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("resourceName", resourceName)
                .append("cloudwatchAlarms", cloudwatchAlarms)
                .toString();
    }
}
