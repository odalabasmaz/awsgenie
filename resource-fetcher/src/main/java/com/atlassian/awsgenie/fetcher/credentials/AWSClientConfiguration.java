package com.atlassian.awsgenie.fetcher.credentials;

public interface AWSClientConfiguration {

    String getAssumeRoleArn();

    String getRegion();
}
