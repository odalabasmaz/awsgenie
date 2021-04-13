package com.atlassian.comparator.analyser;

import com.atlassian.awstool.terminate.FetchResources;
import com.atlassian.comparator.ResourceComparator;

import java.util.List;

import static java.lang.Thread.sleep;

public abstract class BaseResourceAnalyzer {
    protected final ResourceComparator comparator;
    protected final FetchResources sourceResourceFetcher;
    protected final FetchResources targetResourceFetcher;
    private final long sleepBetweenIterations;

    public BaseResourceAnalyzer(ResourceComparator comparator,
                                FetchResources sourceResourceFetcher,
                                FetchResources targetResourceFetcher,
                                long sleepBetweenIterations) {
        this.comparator = comparator;
        this.sourceResourceFetcher = sourceResourceFetcher;
        this.targetResourceFetcher = targetResourceFetcher;
        this.sleepBetweenIterations = sleepBetweenIterations;
    }

    public void run() throws Exception {
        while (!comparator.isFinished()) {
            processCommonQueue();
            sleep(sleepBetweenIterations);
        }
        processCommonQueue();
    }

    abstract void processCommonQueue() throws Exception;
}