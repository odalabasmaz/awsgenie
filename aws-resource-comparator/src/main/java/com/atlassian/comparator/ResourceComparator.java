package com.atlassian.comparator;

public class ResourceComparator {
    ResourceProducer resourceProducerA;
    ResourceProducer resourceProducerB;
    ResourceAnalyzer resourceAnalyzer;
    ResourceQueue valueCommon;

    // write baseJob and extend there
    public void run() {
        /*isRunning = true;
        while (resourceProducerA.isRunning() && resourceProducerB.isRunning()) {
            ResourceQueue valueA = resourceProducerA.getValue();
            ResourceQueue valueB = resourceProducerB.getValue();
            //compare ->
            resourceAnalyzer.submit();
        }
        ResourceQueue valueA = resourceProducerA.dump();
        ResourceQueue valueB = resourceProducerB.dump();
        //compare ->
        resourceAnalyzer.submit();
        resourceAnalyzer.Diff();
        isRunning = false;*/
    }
    // ResourceQueue
    //run method
    // check producers queue -> if exists compare queue message -> check intersection ...


}
