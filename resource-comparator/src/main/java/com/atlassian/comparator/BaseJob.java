package com.atlassian.comparator;

import io.awsgenie.fetcher.Resource;

public abstract class BaseJob<K extends Resource> {

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
