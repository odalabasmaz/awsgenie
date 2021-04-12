package com.atlassian.comparator;

import java.util.concurrent.ThreadPoolExecutor;

public class ResourceComparator extends BaseExecutor {
    public static final int THREAD_POOL_CAPACITY = 2;
    public static final int MAX_NUMBER_OF_THREADS = 2;
    public static final int MAX_QUEUE_SIZE = 2;
    private final ResourceProducer resourceProducerA;
    private final ResourceProducer resourceProducerB;
    private final ResourceAnalyzer resourceAnalyzer;

    private ThreadPoolExecutor executors;

    public ResourceComparator(ResourceProducer resourceProducerA,
                              ResourceProducer resourceProducerB,
                              ResourceAnalyzer resourceAnalyzer) {
        super(MAX_NUMBER_OF_THREADS, MAX_QUEUE_SIZE);
        this.resourceProducerA = resourceProducerA;
        this.resourceProducerB = resourceProducerB;
        this.resourceAnalyzer = resourceAnalyzer;
    }

    //TODO: write baseJob and extend there
    public void run() throws InterruptedException {


        while (resourceProducerA.isRunning() && resourceProducerB.isRunning()) {
            ResourceQueue valueA = resourceProducerA.getValue();
            ResourceQueue valueB = resourceProducerB.getValue();
            //compare ->
            resourceAnalyzer.submit();
            Thread.sleep(2000);
        }
        ResourceQueue valueA = resourceProducerA.dump();
        ResourceQueue valueB = resourceProducerB.dump();
        //compare ->
        resourceAnalyzer.submit();
        resourceAnalyzer.Diff();
    }

    // ResourceQueue
    //run method
    // check producers queue -> if exists compare queue message -> check intersection ...
}
