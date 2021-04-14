package io.github.odalabasmaz.awsgenie.fetcher.lambda;

import io.github.odalabasmaz.awsgenie.fetcher.Resource;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.LinkedHashSet;
import java.util.Objects;

public class LambdaResource extends Resource {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LambdaResource that = (LambdaResource) o;
        return Objects.equals(resourceName, that.resourceName) && Objects.equals(cloudwatchAlarms, that.cloudwatchAlarms)
                && Objects.equals(snsTriggers, that.snsTriggers) && Objects.equals(cloudwatchRules, that.cloudwatchRules)
                && Objects.equals(cloudwatchRuleTargets, that.cloudwatchRuleTargets)
                && Objects.equals(eventSourceMappings, that.eventSourceMappings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceName, cloudwatchAlarms, snsTriggers, cloudwatchRules, cloudwatchRuleTargets, eventSourceMappings);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("resourceName", resourceName)
                .append("cloudwatchAlarms", cloudwatchAlarms)
                .append("snsTriggers", snsTriggers)
                .append("cloudwatchRules", cloudwatchRules)
                .append("cloudwatchRuleTargets", cloudwatchRuleTargets)
                .append("eventSourceMappings", eventSourceMappings)
                .toString();
    }
}
