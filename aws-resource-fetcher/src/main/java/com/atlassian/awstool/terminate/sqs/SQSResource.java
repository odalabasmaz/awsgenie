package com.atlassian.awstool.terminate.sqs;

import com.atlassian.awstool.terminate.AWSResource;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.LinkedHashSet;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SQSResource that = (SQSResource) o;
        return Objects.equals(resourceName, that.resourceName) && Objects.equals(cloudwatchAlarms, that.cloudwatchAlarms) && Objects.equals(snsSubscriptions, that.snsSubscriptions) && Objects.equals(lambdaTriggers, that.lambdaTriggers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceName, cloudwatchAlarms, snsSubscriptions, lambdaTriggers);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("resourceName", resourceName)
                .append("cloudwatchAlarms", cloudwatchAlarms)
                .append("snsSubscriptions", snsSubscriptions)
                .append("lambdaTriggers", lambdaTriggers)
                .toString();
    }
}
