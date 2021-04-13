package com.atlassian.comparator;

import com.atlassian.awstool.terminate.AWSResource;

public abstract class BaseJob<K extends AWSResource> {

    private boolean isFinished = false;

    public abstract void _run() throws Exception;

    public void run() throws Exception {
        isFinished = false;
        _run();
        isFinished = true;
    }

    public boolean isFinished() {
        return isFinished;
    }
}
