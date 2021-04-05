package com.atlassian.awsterminator;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Orhun Dalabasmaz
 * @version 10.03.2021
 */

public class TerminateResourcesRequest {

    @Parameter(names = {"--region", "-r"}, description = "AWS region", required = true)
    private String region;

    @Parameter(names = {"--service", "-s"}, description = "AWS service name: sqs, lambda, dynamodb, cloudwatch, etc.", required = true)
    private String service;

    @Parameter(names = {"--resources", "-res"}, description = "AWS resource names (semicolon-separated, whitespaces converted to +), ie. a;b+c;d", required = true)
    private String resources;

    @Parameter(names = {"--ticket", "-t"}, description = "Related OGSD ticket id, ie. OGSD-1234", required = true)
    private String ticket;

    @Parameter(names = {"--apply", "-a"}, description = "Apply the changes, dry-run by default")
    private boolean apply = false;

    @Parameter(names = {"--assume-role-arn", "-ara"}, description = "IAM Role ARN to assume")
    private String assumeRoleArn = "";

    public void fromSystemArgs(String[] args) {
        new JCommander(this, args);
    }

    public String getRegion() {
        return region;
    }

    public TerminateResourcesRequest setRegion(String region) {
        this.region = region;
        return this;
    }

    public String getService() {
        return service;
    }

    public TerminateResourcesRequest setService(String service) {
        this.service = service;
        return this;
    }

    public List<String> getResources() {
        return Arrays.stream(resources.split(";"))
                .map(String::trim)
                .map(r -> r.replaceAll("\\+", " "))
                .distinct()
                .collect(Collectors.toList());
    }

    public TerminateResourcesRequest setResources(String resources) {
        this.resources = resources;
        return this;
    }

    public String getTicket() {
        return ticket;
    }

    public TerminateResourcesRequest setTicket(String ticket) {
        this.ticket = ticket;
        return this;
    }

    public boolean isApply() {
        return apply;
    }

    public TerminateResourcesRequest setApply(boolean apply) {
        this.apply = apply;
        return this;
    }

    public String getAssumeRoleArn() {
        return assumeRoleArn;
    }

    public TerminateResourcesRequest setAssumeRoleArn(String assumeRoleArn) {
        this.assumeRoleArn = assumeRoleArn;
        return this;
    }
}
