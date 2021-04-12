package com.atlassian.awstool.terminate.lambda;

import com.atlassian.awstool.terminate.AWSResource;

import java.util.LinkedHashSet;

public class LambdaResource extends AWSResource {
    private String resourceName;
    private LinkedHashSet<String> cloudwatchAlarms = new LinkedHashSet<>();
    private LinkedHashSet<String> snsTriggers = new LinkedHashSet<>();
    private LinkedHashSet<String> cloudwatchRules = new LinkedHashSet<>();
    private LinkedHashSet<String> cloudwatchRuleTargets = new LinkedHashSet<>();
    private LinkedHashSet<String> eventSourceMappings = new LinkedHashSet<>();

    @Override
    public String getResourceName() {
        return resourceName;
    }

    public LambdaResource setResourceName(String resourceName) {
        this.resourceName = resourceName;
        return this;
    }

    public LinkedHashSet<String> getCloudwatchAlarms() {
        return cloudwatchAlarms;
    }

    public LambdaResource setCloudwatchAlarms(LinkedHashSet<String> cloudwatchAlarms) {
        this.cloudwatchAlarms = cloudwatchAlarms;
        return this;
    }

    public LinkedHashSet<String> getSnsTriggers() {
        return snsTriggers;
    }

    public LambdaResource setSnsTriggers(LinkedHashSet<String> snsTriggers) {
        this.snsTriggers = snsTriggers;
        return this;
    }

    public LinkedHashSet<String> getCloudwatchRules() {
        return cloudwatchRules;
    }

    public LambdaResource setCloudwatchRules(LinkedHashSet<String> cloudwatchRules) {
        this.cloudwatchRules = cloudwatchRules;
        return this;
    }

    public LinkedHashSet<String> getCloudwatchRuleTargets() {
        return cloudwatchRuleTargets;
    }

    public LambdaResource setCloudwatchRuleTargets(LinkedHashSet<String> cloudwatchRuleTargets) {
        this.cloudwatchRuleTargets = cloudwatchRuleTargets;
        return this;
    }

    public LinkedHashSet<String> getEventSourceMappings() {
        return eventSourceMappings;
    }

    public LambdaResource setEventSourceMappings(LinkedHashSet<String> eventSourceMappings) {
        this.eventSourceMappings = eventSourceMappings;
        return this;
    }
}
