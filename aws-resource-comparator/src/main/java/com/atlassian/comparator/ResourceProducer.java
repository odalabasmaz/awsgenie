package com.atlassian.comparator;

import com.atlassian.awstool.terminate.AWSResource;
import com.atlassian.awstool.terminate.FetchResources;

public class ResourceProducer<K extends AWSResource> extends BaseJob<K> {
    ResourceQueue<String> queueA;

    public ResourceProducer(FetchResources<K> fetchResources, ResourceQueue<String> queueA) {
        super(fetchResources);
        this.queueA = queueA;
    }


    @Override
    public void _run(FetchResources<K> fetchResources) throws Exception {
        fetchResources.listResources(resources -> queueA.addAll(resources));
    }

    public ResourceQueue<String> getValue() {
        return new ResourceQueue<>(queueA.getAll());
    }

}
