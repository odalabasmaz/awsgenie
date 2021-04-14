package io.awsgenie.fetcher;


public abstract class ResourceFetcherWithProvider {

    private ResourceFetcherConfiguration configuration;


    public ResourceFetcherWithProvider(ResourceFetcherConfiguration configuration) {
        this.configuration = configuration;
    }

    public ResourceFetcherConfiguration getConfiguration() {
        return configuration;
    }
}
