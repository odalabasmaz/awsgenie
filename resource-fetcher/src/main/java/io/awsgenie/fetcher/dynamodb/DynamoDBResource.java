package io.awsgenie.fetcher.dynamodb;

import io.awsgenie.fetcher.Resource;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.LinkedHashSet;
import java.util.Objects;

public class DynamoDBResource extends Resource {
    private String resourceName;
    private LinkedHashSet<String> cloudwatchAlarmList = new LinkedHashSet<>();


    @Override
    public String getResourceName() {
        return resourceName;
    }

    public DynamoDBResource setResourceName(String resourceName) {
        this.resourceName = resourceName;
        return this;
    }

    public LinkedHashSet<String> getCloudwatchAlarmList() {
        return cloudwatchAlarmList;
    }

    public DynamoDBResource setCloudwatchAlarmList(LinkedHashSet<String> cloudwatchAlarmList) {
        this.cloudwatchAlarmList = cloudwatchAlarmList;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DynamoDBResource that = (DynamoDBResource) o;
        return Objects.equals(resourceName, that.resourceName) && Objects.equals(cloudwatchAlarmList, that.cloudwatchAlarmList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceName, cloudwatchAlarmList);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("resourceName", resourceName)
                .append("cloudwatchAlarmList", cloudwatchAlarmList)
                .toString();
    }
}
