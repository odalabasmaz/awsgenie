package com.atlassian.awstool.terminate.sns;

import com.atlassian.awstool.terminate.AWSResource;

import java.util.LinkedHashSet;

public class SNSResource extends AWSResource {
    private String resourceName;
    private LinkedHashSet<String> cloudwatchAlarms = new LinkedHashSet<>();
    private Double publishCountInLastWeek;

    @Override
    public String getResourceName() {
        return resourceName;
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

    public Double getPublishCountInLastWeek() {
        return publishCountInLastWeek;
    }

    public SNSResource setPublishCountInLastWeek(Double publishCountInLastWeek) {
        this.publishCountInLastWeek = publishCountInLastWeek;
        return this;
    }
}
