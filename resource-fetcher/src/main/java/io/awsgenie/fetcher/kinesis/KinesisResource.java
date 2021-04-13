package io.awsgenie.fetcher.kinesis;

import io.awsgenie.fetcher.Resource;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.LinkedHashSet;
import java.util.Objects;

/**
 * @author Celal Emre CICEK
 * @version 7.04.2021
 */

public class KinesisResource extends Resource {
    private String resourceName;
    private LinkedHashSet<String> cloudwatchAlarmList = new LinkedHashSet<>();

    @Override
    public String getResourceName() {
        return resourceName;
    }

    @Override
    public Object getResourceObject() {
        return null;
    }

    public KinesisResource setResourceName(String resourceName) {
        this.resourceName = resourceName;
        return this;
    }

    public LinkedHashSet<String> getCloudwatchAlarmList() {
        return cloudwatchAlarmList;
    }

    public KinesisResource setCloudwatchAlarmList(LinkedHashSet<String> cloudwatchAlarmList) {
        this.cloudwatchAlarmList = cloudwatchAlarmList;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KinesisResource that = (KinesisResource) o;
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
