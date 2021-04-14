package com.atlassian.awsgenie.terminator.configuration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Objects;

/**
 * @author Celal Emre CICEK
 * @version 6.04.2021
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class FileConfiguration implements Configuration {
    private String region;
    private String service;
    private String resources;
    private int lastUsage = 7;
    private String description;
    private String assumeRoleArn;
    private boolean force;

    @Override
    public FileConfiguration cloneMe() {
        return new FileConfiguration()
                .setRegion(this.region)
                .setService(this.service)
                .setResources(this.resources)
                .setLastUsage(this.lastUsage)
                .setDescription(this.description)
                .setAssumeRoleArn(this.assumeRoleArn)
                .setForce(this.force);
    }

    @Override
    public String getRegion() {
        return region;
    }

    @Override
    public FileConfiguration setRegion(String region) {
        this.region = region;
        return this;
    }

    @Override
    public String getService() {
        return service;
    }

    @Override
    public FileConfiguration setService(String service) {
        this.service = service;
        return this;
    }

    @Override
    public String getResources() {
        return resources;
    }

    @Override
    public FileConfiguration setResources(String resources) {
        this.resources = resources;
        return this;
    }

    @Override
    public int getLastUsage() {
        return lastUsage;
    }

    @Override
    public FileConfiguration setLastUsage(int lastUsage) {
        this.lastUsage = lastUsage;
        return this;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public FileConfiguration setDescription(String ticket) {
        this.description = ticket;
        return this;
    }

    @Override
    public boolean isForce() {
        return force;
    }

    @Override
    public FileConfiguration setForce(boolean force) {
        this.force = force;
        return this;
    }


    @Override
    public String getAssumeRoleArn() {
        return assumeRoleArn;
    }

    @Override
    public FileConfiguration setAssumeRoleArn(String assumeRoleArn) {
        this.assumeRoleArn = assumeRoleArn;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileConfiguration that = (FileConfiguration) o;
        return Objects.equals(region, that.region)
                && Objects.equals(service, that.service)
                && Objects.equals(resources, that.resources)
                && Objects.equals(lastUsage, that.lastUsage)
                && Objects.equals(description, that.description)
                && Objects.equals(assumeRoleArn, that.assumeRoleArn)
                && Objects.equals(force, that.force);
    }

    @Override
    public int hashCode() {
        return Objects.hash(region, service, resources, lastUsage, description, assumeRoleArn, force);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("region", region)
                .append("service", service)
                .append("resources", resources)
                .append("lastUsage", lastUsage)
                .append("ticket", description)
                .append("assumeRoleArn", assumeRoleArn)
                .append("force", force)
                .toString();
    }
}
