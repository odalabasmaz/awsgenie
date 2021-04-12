package com.atlassian.awsterminator.terminate;

import credentials.AwsClientConfiguration;

public abstract class TerminateResourcesWithProvider {

    private AwsClientConfiguration configuration;

    public TerminateResourcesWithProvider(AwsClientConfiguration configuration) {
        this.configuration = configuration;
    }

    public AwsClientConfiguration getConfiguration() {
        return configuration;
    }
}
