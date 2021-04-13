package com.atlassian.comparator;

import com.atlassian.awstool.terminate.FetchResources;

import java.util.List;

import static java.lang.Thread.sleep;

public class ResourceAnalyzer {
    private final ResourceComparator comparator;
    private final FetchResources sourceResourceFetcher;
    private final FetchResources targetResourceFetcher;
    private final long sleepBetweenIterations;

    public ResourceAnalyzer(ResourceComparator comparator,
                            FetchResources sourceResourceFetcher,
                            FetchResources targetResourceFetcher,
                            long sleepBetweenIterations) {
        this.comparator = comparator;
        this.sourceResourceFetcher = sourceResourceFetcher;
        this.targetResourceFetcher = targetResourceFetcher;
        this.sleepBetweenIterations = sleepBetweenIterations;
    }

    public void run() throws Exception {
        while (comparator.isRunning()) {
            processCommonQueue();
            sleep(sleepBetweenIterations);
        }
        processCommonQueue();
    }

    private void processCommonQueue() {
        List<String> commonObjects = comparator.getCommonQueue().getAll();
        for (String commonObject : commonObjects) {
            //TODO: fetch resources here and write results to output
            System.out.println("commonObject: " + commonObject);
        }
        comparator.getCommonQueue().removeAll(commonObjects);
    }
}