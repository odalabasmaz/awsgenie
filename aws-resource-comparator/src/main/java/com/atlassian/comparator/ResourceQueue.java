package com.atlassian.comparator;

import java.util.concurrent.ConcurrentLinkedQueue;

public class ResourceQueue {
    ConcurrentLinkedQueue<String> resourceQueue = new ConcurrentLinkedQueue<>();

    public String pop() {
        return resourceQueue.poll();
    }

    public void put(String resource) {
        resourceQueue.add(resource);
    }
}
