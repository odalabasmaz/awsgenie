package com.atlassian.awstool.terminate.kinesis;

import com.atlassian.awstool.terminate.AWSResource;

import java.util.LinkedHashSet;

/**
 * @author Celal Emre CICEK
 * @version 7.04.2021
 */

public class KinesisResource extends AWSResource {
    private String resourceName;
    private LinkedHashSet<String> cloudwatchAlarmList = new LinkedHashSet<>();

    @Override
    public String getResourceName() {
        return resourceName;
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
}
