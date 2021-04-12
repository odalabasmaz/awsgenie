package com.atlassian.awstool.terminate;



public abstract class FetchResourcesWithProvider{

    private FetcherConfiguration configuration;


    public FetchResourcesWithProvider(FetcherConfiguration configuration) {
        this.configuration = configuration;
    }

    public FetcherConfiguration getConfiguration() {
        return configuration;
    }
}
