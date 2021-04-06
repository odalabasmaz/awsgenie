package com.atlassian.awsterminator.configuration;

import com.atlassian.awsterminator.exception.ConfigurationValidationException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang3.StringUtils;
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
    private String ticket;
    private String assumeRoleArn;

    @Override
    public FileConfiguration cloneMe() {
        return new FileConfiguration()
                .setRegion(this.region)
                .setService(this.service)
                .setResources(this.resources)
                .setTicket(this.ticket)
                .setAssumeRoleArn(this.assumeRoleArn);
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
    public String getTicket() {
        return ticket;
    }

    @Override
    public FileConfiguration setTicket(String ticket) {
        this.ticket = ticket;
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
        return Objects.equals(region, that.region) && Objects.equals(service, that.service) &&
                Objects.equals(resources, that.resources) && Objects.equals(ticket, that.ticket) &&
                Objects.equals(assumeRoleArn, that.assumeRoleArn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(region, service, resources, ticket, assumeRoleArn);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("region", region)
                .append("service", service)
                .append("resources", resources)
                .append("ticket", ticket)
                .append("assumeRoleArn", assumeRoleArn)
                .toString();
    }
}