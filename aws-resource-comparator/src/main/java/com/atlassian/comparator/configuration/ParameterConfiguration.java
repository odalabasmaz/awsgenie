package com.atlassian.comparator.configuration;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Objects;

public class ParameterConfiguration {
    @Parameter(names = {"--sourceRegion", "-sr"}, description = "Source AWS region")
    private String sourceRegion;

    @Parameter(names = {"--targetRegion", "-tr"}, description = "Target AWS region")
    private String targetRegion;

    @Parameter(names = {"--service", "-s"}, description = "AWS service name: sqs, lambda, dynamodb, cloudwatch, etc.")
    private String service;

    @Parameter(names = {"--sourceAssumeRoleArn", "-sara"}, description = "IAM Role ARN to assume in source AWS account")
    private String sourceAssumeRoleArn;

    @Parameter(names = {"--targetAssumeRoleArn", "-sara"}, description = "IAM Role ARN to assume in target AWS account")
    private String targetAssumeRoleArn;

    public void fromSystemArgs(String[] args) {
        new JCommander(this, args);
    }

    public String getSourceRegion() {
        return sourceRegion;
    }

    public ParameterConfiguration setSourceRegion(String sourceRegion) {
        this.sourceRegion = sourceRegion;
        return this;
    }

    public String getTargetRegion() {
        return targetRegion;
    }

    public ParameterConfiguration setTargetRegion(String targetRegion) {
        this.targetRegion = targetRegion;
        return this;
    }

    public String getService() {
        return service;
    }

    public ParameterConfiguration setService(String service) {
        this.service = service;
        return this;
    }

    public String getSourceAssumeRoleArn() {
        return sourceAssumeRoleArn;
    }

    public ParameterConfiguration setSourceAssumeRoleArn(String sourceAssumeRoleArn) {
        this.sourceAssumeRoleArn = sourceAssumeRoleArn;
        return this;
    }

    public String getTargetAssumeRoleArn() {
        return targetAssumeRoleArn;
    }

    public ParameterConfiguration setTargetAssumeRoleArn(String targetAssumeRoleArn) {
        this.targetAssumeRoleArn = targetAssumeRoleArn;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParameterConfiguration that = (ParameterConfiguration) o;
        return Objects.equals(sourceRegion, that.sourceRegion) && Objects.equals(targetRegion, that.targetRegion) && Objects.equals(service, that.service) && Objects.equals(sourceAssumeRoleArn, that.sourceAssumeRoleArn) && Objects.equals(targetAssumeRoleArn, that.targetAssumeRoleArn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceRegion, targetRegion, service, sourceAssumeRoleArn, targetAssumeRoleArn);
    }

    @Override
    public String toString() {
        return "ParameterConfiguration{" +
                "sourceRegion='" + sourceRegion + '\'' +
                ", targetRegion='" + targetRegion + '\'' +
                ", service='" + service + '\'' +
                ", sourceAssumeRoleArn='" + sourceAssumeRoleArn + '\'' +
                ", targetAssumeRoleArn='" + targetAssumeRoleArn + '\'' +
                '}';
    }
}
