package com.atlassian.awsgenie.fetcher;

import com.atlassian.awsgenie.fetcher.credentials.AWSClientConfiguration;

public class ResourceFetcherConfiguration implements AWSClientConfiguration {

    private final String assumeRoleArn;
    private final String region;

    public ResourceFetcherConfiguration(String assumeRoleArn, String region) {
        this.assumeRoleArn = assumeRoleArn;
        this.region = region;
    }

    public ResourceFetcherConfiguration(AWSClientConfiguration awsClientConfiguration) {
        this.assumeRoleArn = awsClientConfiguration.getAssumeRoleArn();
        this.region = awsClientConfiguration.getRegion();
    }

    @Override
    public String getAssumeRoleArn() {
        return assumeRoleArn;
    }

    @Override
    public String getRegion() {
        return region;
    }
}
