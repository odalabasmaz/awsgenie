package com.atlassian.comparator.configuration;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Objects;

/**
 * @author Orhun Dalabasmaz
 * @version 10.03.2021
 */

public class ParameterConfiguration {
    @Parameter(names = {"--region", "-r"}, description = "AWS region")
    private String region;

    @Parameter(names = {"--service", "-s"}, description = "AWS service name: sqs, lambda, dynamodb, cloudwatch, etc.")
    private String service;

    @Parameter(names = {"--assume-role-arn", "-ara"}, description = "IAM Role ARN to assume")
    private String assumeRoleArn;

    public void fromSystemArgs(String[] args) {
        new JCommander(this, args);
    }

    public String getRegion() {
        return region;
    }

    public ParameterConfiguration setRegion(String region) {
        this.region = region;
        return this;
    }

    public String getService() {
        return service;
    }

    public ParameterConfiguration setService(String service) {
        this.service = service;
        return this;
    }

    public String getAssumeRoleArn() {
        return assumeRoleArn;
    }

    public ParameterConfiguration setAssumeRoleArn(String assumeRoleArn) {
        this.assumeRoleArn = assumeRoleArn;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParameterConfiguration that = (ParameterConfiguration) o;
        return Objects.equals(region, that.region) && Objects.equals(service, that.service) && Objects.equals(assumeRoleArn, that.assumeRoleArn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(region, service, assumeRoleArn);
    }

    @Override
    public String toString() {
        return "ParameterConfiguration{" +
                "region='" + region + '\'' +
                ", service='" + service + '\'' +
                ", assumeRoleArn='" + assumeRoleArn + '\'' +
                '}';
    }
}
