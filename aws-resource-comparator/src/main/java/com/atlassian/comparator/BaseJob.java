package com.atlassian.comparator;

import com.atlassian.awstool.terminate.AWSResource;

public abstract class BaseJob<K extends AWSResource> {

    private boolean isRunning = false;

    public abstract void _run() throws Exception;

    public void run() throws Exception {
        isRunning = true;
        _run();
        isRunning = false;
    }

    public boolean isRunning() {
        return isRunning;
    }
}
