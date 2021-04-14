package io.awsgenie.terminator;

import io.awsgenie.fetcher.credentials.AWSClientConfiguration;

public abstract class ResourceTerminatorWithProvider {
    private final AWSClientConfiguration configuration;

    public ResourceTerminatorWithProvider(AWSClientConfiguration configuration) {
        this.configuration = configuration;
    }

    public AWSClientConfiguration getConfiguration() {
        return configuration;
    }
}
