package com.atlassian.awstool.terminate.sqs;

import com.atlassian.awstool.terminate.AWSResource;

import java.util.LinkedHashSet;

public class SQSResource extends AWSResource {
    private String resourceName;
    private LinkedHashSet<String> cloudwatchAlarms = new LinkedHashSet<>();
    private LinkedHashSet<String> snsSubscriptions = new LinkedHashSet<>();
    private LinkedHashSet<String> lambdaTriggers = new LinkedHashSet<>();

    @Override
    public String getResourceName() {
        return resourceName;
    }

    public SQSResource setResourceName(String resourceName) {
        this.resourceName = resourceName;
        return this;
    }

    public LinkedHashSet<String> getCloudwatchAlarms() {
        return cloudwatchAlarms;
    }

    public SQSResource setCloudwatchAlarms(LinkedHashSet<String> cloudwatchAlarms) {
        this.cloudwatchAlarms = cloudwatchAlarms;
        return this;
    }

    public LinkedHashSet<String> getSnsSubscriptions() {
        return snsSubscriptions;
    }

    public SQSResource setSnsSubscriptions(LinkedHashSet<String> snsSubscriptions) {
        this.snsSubscriptions = snsSubscriptions;
        return this;
    }

    public LinkedHashSet<String> getLambdaTriggers() {
        return lambdaTriggers;
    }

    public SQSResource setLambdaTriggers(LinkedHashSet<String> lambdaTriggers) {
        this.lambdaTriggers = lambdaTriggers;
        return this;
    }
}
