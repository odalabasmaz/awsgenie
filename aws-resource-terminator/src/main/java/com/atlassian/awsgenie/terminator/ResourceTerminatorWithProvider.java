package com.atlassian.awsgenie.terminator;

import com.atlassian.awsgenie.fetcher.credentials.AWSClientConfiguration;

public abstract class ResourceTerminatorWithProvider {
    private final AWSClientConfiguration configuration;

    public ResourceTerminatorWithProvider(AWSClientConfiguration configuration) {
        this.configuration = configuration;
    }

    public AWSClientConfiguration getConfiguration() {
        return configuration;
    }
}
