package io.awsgenie.fetcher.credentials;

public interface AWSClientConfiguration {

    String getAssumeRoleArn();

    String getRegion();
}
