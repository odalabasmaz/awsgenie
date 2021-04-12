package com.atlassian.comparator;

import com.atlassian.awstool.terminate.AWSResource;
import com.atlassian.awstool.terminate.FetchResources;

public abstract class BaseJob<K extends AWSResource> {

    private boolean isRunning = false;
    private final FetchResources<K> fetchResources;

    public BaseJob(FetchResources<K> fetchResources) {
        this.fetchResources = fetchResources;
    }

    public abstract void _run(FetchResources<K> fetchResources) throws Exception;

    public void run() throws Exception {
        start();
    }

    public void start() throws Exception {
        isRunning = true;
        _run(fetchResources);
        isRunning = false;
    }

    public boolean isRunning() {
        return isRunning;
    }
}
