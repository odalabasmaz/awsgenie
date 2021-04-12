package com.atlassian.awstool.terminate;

import credentials.AwsClientConfiguration;

public class FetcherConfiguration implements AwsClientConfiguration {

    private final String assumeRoleArn;
    private final String region;

    public FetcherConfiguration(String assumeRoleArn, String region) {
        this.assumeRoleArn = assumeRoleArn;
        this.region = region;
    }

    public FetcherConfiguration(AwsClientConfiguration awsClientConfiguration) {
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
