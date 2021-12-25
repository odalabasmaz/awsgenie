package io.github.odalabasmaz.awsgenie.fetcher.credentials;

public interface AWSClientConfiguration {

    String getAssumeRoleArn();

    String getRegion();
}
