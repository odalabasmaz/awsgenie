package com.atlassian.comparator;

public class ResourceProducer extends BaseJob {
    ResourceQueue queue;
    //ResourceQueue
    //run method  -> fetchResource -> list -> write to Queue
    //isDone

    public ResourceProducer(ResourceQueue queue) {
        this.queue = queue;
    }

    public void run() {
        //fetcher -> list method -> consumer.
    }

    public ResourceQueue getQueue() {
        return queue;
    }

    @Override
    public boolean isRunning() {
        return false;
    }
}
