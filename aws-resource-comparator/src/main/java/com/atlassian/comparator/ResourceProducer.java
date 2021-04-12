package com.atlassian.comparator;

public class ResourceProducer {
    ResourceQueue queueA;
    //ResourceQueue
    //run method  -> fetchResource -> list -> write to Queue
    //isDone


    public void run(){
        //fetcher -> list method -> consumer.
        //
    }
    public ResourceQueue getValue() {
        return queueA;
    }
    public boolean isRunning(){
        return false;
    }
}
