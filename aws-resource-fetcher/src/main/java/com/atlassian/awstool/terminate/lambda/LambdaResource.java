package com.atlassian.awstool.terminate.lambda;

import com.atlassian.awstool.terminate.AWSResource;

import java.util.LinkedHashSet;

public class LambdaResource extends AWSResource {
    private String resourceName;
    private LinkedHashSet<String> cloudwatchAlarmsToDelete = new LinkedHashSet<>();
    private LinkedHashSet<String> snsTriggersToDelete = new LinkedHashSet<>();
    private LinkedHashSet<String> cloudwatchRulesToDelete = new LinkedHashSet<>();
    private LinkedHashSet<String> cloudwatchRuleTargetsToDelete = new LinkedHashSet<>();
    private LinkedHashSet<String> eventSourceMappingsToDelete = new LinkedHashSet<>();

    @Override
    public String getResourceName() {
        return resourceName;
    }

    public LambdaResource setResourceName(String resourceName) {
        this.resourceName = resourceName;
        return this;
    }

    public LinkedHashSet<String> getCloudwatchAlarmsToDelete() {
        return cloudwatchAlarmsToDelete;
    }

    public LambdaResource setCloudwatchAlarmsToDelete(LinkedHashSet<String> cloudwatchAlarmsToDelete) {
        this.cloudwatchAlarmsToDelete = cloudwatchAlarmsToDelete;
        return this;
    }

    public LinkedHashSet<String> getSnsTriggersToDelete() {
        return snsTriggersToDelete;
    }

    public LambdaResource setSnsTriggersToDelete(LinkedHashSet<String> snsTriggersToDelete) {
        this.snsTriggersToDelete = snsTriggersToDelete;
        return this;
    }

    public LinkedHashSet<String> getCloudwatchRulesToDelete() {
        return cloudwatchRulesToDelete;
    }

    public LambdaResource setCloudwatchRulesToDelete(LinkedHashSet<String> cloudwatchRulesToDelete) {
        this.cloudwatchRulesToDelete = cloudwatchRulesToDelete;
        return this;
    }

    public LinkedHashSet<String> getCloudwatchRuleTargetsToDelete() {
        return cloudwatchRuleTargetsToDelete;
    }

    public LambdaResource setCloudwatchRuleTargetsToDelete(LinkedHashSet<String> cloudwatchRuleTargetsToDelete) {
        this.cloudwatchRuleTargetsToDelete = cloudwatchRuleTargetsToDelete;
        return this;
    }

    public LinkedHashSet<String> getEventSourceMappingsToDelete() {
        return eventSourceMappingsToDelete;
    }

    public LambdaResource setEventSourceMappingsToDelete(LinkedHashSet<String> eventSourceMappingsToDelete) {
        this.eventSourceMappingsToDelete = eventSourceMappingsToDelete;
        return this;
    }
}
