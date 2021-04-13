package com.atlassian.comparator;

import com.atlassian.awstool.terminate.AWSResource;
import com.atlassian.awstool.terminate.FetchResources;

public class ResourceProducer<K extends AWSResource> extends BaseJob<K> {
    ResourceQueue<String> queueA;
    private final FetchResources<K> fetchResources;

    public ResourceProducer(FetchResources<K> fetchResources, ResourceQueue<String> queueA) {
        this.fetchResources = fetchResources;
        this.queueA = queueA;
    }

    @Override
    public void _run() throws Exception {
        fetchResources.listResources(resources -> queueA.addAll(resources));
        queueA.setFinishedPopulating(true);
    }
}
