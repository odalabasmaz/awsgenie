package com.atlassian.awstool.terminate.dynamodb;

import com.atlassian.awstool.terminate.AWSResource;

import java.util.LinkedHashSet;

public class DynamodbResource extends AWSResource {
    private String resourceName;
    private double totalUsage = 0;
    private LinkedHashSet<String> cloudwatchAlarmList = new LinkedHashSet<>();

    @Override
    public String getResourceName() {
        return resourceName;
    }

    public DynamodbResource setResourceName(String resourceName) {
        this.resourceName = resourceName;
        return this;
    }

    public double getTotalUsage() {
        return totalUsage;
    }

    public DynamodbResource setTotalUsage(double totalUsage) {
        this.totalUsage = totalUsage;
        return this;
    }

    public LinkedHashSet<String> getCloudwatchAlarmList() {
        return cloudwatchAlarmList;
    }

    public DynamodbResource setCloudwatchAlarmList(LinkedHashSet<String> cloudwatchAlarmList) {
        this.cloudwatchAlarmList = cloudwatchAlarmList;
        return this;
    }
}
