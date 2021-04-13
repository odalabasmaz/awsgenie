package com.atlassian.awsterminator.configuration;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Objects;

/**
 * @author Orhun Dalabasmaz
 * @version 10.03.2021
 */

public class ParameterConfiguration implements Configuration {
    @Parameter(names = {"--region", "-r"}, description = "AWS region")
    private String region;

    @Parameter(names = {"--service", "-s"}, description = "AWS service name: sqs, lambda, dynamodb, cloudwatch, etc.")
    private String service;

    @Parameter(names = {"--resources", "-res"}, description = "AWS resource names (semicolon-separated, whitespaces converted to +), ie. a;b+c;d")
    private String resources;

    @Parameter(names = {"--description", "-d"}, description = "Description")
    private String description;

    @Parameter(names = {"--assume-role-arn", "-ara"}, description = "IAM Role ARN to assume")
    private String assumeRoleArn;

    @Parameter(names = {"--apply"}, description = "Apply the changes, dry-run by default")
    private boolean apply = false;

    @Parameter(names = {"--force"}, description = "Force applying the changes, even the usage confirmed")
    private boolean force = false;

    @Parameter(names = {"--last-usage"}, description = "Check usage for the last 7 days by default")
    private int lastUsage = 7;

    @Parameter(names = {"--configuration-file", "-cf"}, description = "Use configuration file")
    private String configurationFile;

    public void fromSystemArgs(String[] args) {
        new JCommander(this, args);
    }

    @Override
    public ParameterConfiguration cloneMe() {
        return new ParameterConfiguration()
                .setRegion(this.region)
                .setService(this.service)
                .setResources(this.resources)
                .setDescription(this.description)
                .setAssumeRoleArn(this.assumeRoleArn)
                .setApply(this.apply)
                .setForce(this.force)
                .setLastUsage(this.lastUsage)
                .setConfigurationFile(this.configurationFile);
    }

    @Override
    public String getRegion() {
        return region;
    }

    @Override
    public ParameterConfiguration setRegion(String region) {
        this.region = region;
        return this;
    }

    @Override
    public String getService() {
        return service;
    }

    @Override
    public ParameterConfiguration setService(String service) {
        this.service = service;
        return this;
    }

    @Override
    public String getResources() {
        return resources;
    }

    @Override
    public ParameterConfiguration setResources(String resources) {
        this.resources = resources;
        return this;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public ParameterConfiguration setDescription(String ticket) {
        this.description = ticket;
        return this;
    }

    @Override
    public String getAssumeRoleArn() {
        return assumeRoleArn;
    }

    @Override
    public ParameterConfiguration setAssumeRoleArn(String assumeRoleArn) {
        this.assumeRoleArn = assumeRoleArn;
        return this;
    }

    @Override
    public boolean isForce() {
        return force;
    }

    @Override
    public ParameterConfiguration setForce(boolean force) {
        this.force = force;
        return this;
    }

    public int getLastUsage() {
        return lastUsage;
    }

    public ParameterConfiguration setLastUsage(int lastUsage) {
        this.lastUsage = lastUsage;
        return this;
    }

    public boolean isApply() {
        return apply;
    }

    public ParameterConfiguration setApply(boolean apply) {
        this.apply = apply;
        return this;
    }

    public String getConfigurationFile() {
        return configurationFile;
    }

    public ParameterConfiguration setConfigurationFile(String configurationFile) {
        this.configurationFile = configurationFile;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParameterConfiguration that = (ParameterConfiguration) o;
        return apply == that.apply
                && force == that.force
                && Objects.equals(region, that.region)
                && Objects.equals(service, that.service)
                && Objects.equals(resources, that.resources)
                && Objects.equals(description, that.description)
                && Objects.equals(assumeRoleArn, that.assumeRoleArn)
                && Objects.equals(lastUsage, that.lastUsage)
                && Objects.equals(configurationFile, that.configurationFile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(region, service, resources, description, assumeRoleArn, apply, configurationFile);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("region", region)
                .append("service", service)
                .append("resources", resources)
                .append("ticket", description)
                .append("assumeRoleArn", assumeRoleArn)
                .append("apply", apply)
                .append("force", force)
                .append("lastUsage", lastUsage)
                .append("configurationFile", configurationFile)
                .toString();
    }
}
