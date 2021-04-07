package com.atlassian.awstool.terminate.cloudwatch;

import com.atlassian.awstool.terminate.AWSResource;

import java.util.LinkedHashSet;

public class CloudwatchResource extends AWSResource {
    private String resourceName;

    @Override
    public String getResourceName() {
        return resourceName;
    }

    public CloudwatchResource setResourceName(String resourceName) {
        this.resourceName = resourceName;
        return this;
    }
}
