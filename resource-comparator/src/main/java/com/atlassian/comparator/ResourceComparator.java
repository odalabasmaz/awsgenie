package com.atlassian.comparator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.lang.Thread.sleep;

public class ResourceComparator extends BaseJob {
    private final ResourceQueue<String> sourceQueue;
    private final ResourceQueue<String> targetQueue;
    private final ResourceQueue<String> commonQueue;

    private final long sleepBetweenIterations;
    private static final Logger logger = LogManager.getLogger(ResourceComparator.class);

    public ResourceComparator(ResourceQueue<String> sourceQueue,
                              ResourceQueue<String> targetQueue,
                              ResourceQueue<String> commonQueue,
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
            logger.info("extracting common resources");
            extractCommonResources();
            sourceQueue.waitNewMessage(sleepBetweenIterations);
            targetQueue.waitNewMessage(sleepBetweenIterations);
        }
        extractCommonResources();
        commonQueue.setFinishedPopulating(true);
        logger.info("finished extracting common resources");

    }

    public ResourceQueue<String> getCommonQueue() {
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
