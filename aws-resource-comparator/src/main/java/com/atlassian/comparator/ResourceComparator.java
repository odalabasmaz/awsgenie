package com.atlassian.comparator;

import java.util.List;

import static java.lang.Thread.sleep;

public class ResourceComparator extends BaseJob {
    private final ResourceQueue sourceQueue;
    private final ResourceQueue targetQueue;
    private final ResourceQueue commonQueue;

    private final long sleepBetweenIterations;

    public ResourceComparator(ResourceQueue sourceQueue,
                              ResourceQueue targetQueue,
                              ResourceQueue commonQueue,
                              long sleepBetweenIterations
    ) {
        this.sourceQueue = sourceQueue;
        this.targetQueue = targetQueue;
        this.commonQueue = commonQueue;
        this.sleepBetweenIterations = sleepBetweenIterations;
    }

    @Override
    public void _run() throws InterruptedException {
        while (!sourceQueue.isFinishedPopulating() || !targetQueue.isFinishedPopulating()) {
            extractCommonResources();

            sleep(sleepBetweenIterations);
        }
        extractCommonResources();
    }

    public ResourceQueue getCommonQueue() {
        return commonQueue;
    }

    private void extractCommonResources() {
        List<String> sourceResourceList = sourceQueue.getAll();
        List<String> targetResourceList = targetQueue.getAll();
        sourceResourceList.retainAll(targetResourceList);

        commonQueue.addAll(sourceResourceList);
        sourceQueue.removeAll(sourceResourceList);
        targetQueue.removeAll(sourceResourceList);
    }
}
