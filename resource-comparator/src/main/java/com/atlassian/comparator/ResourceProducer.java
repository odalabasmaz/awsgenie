package com.atlassian.comparator;

import io.awsgenie.fetcher.Resource;
import io.awsgenie.fetcher.ResourceFetcher;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ResourceProducer<K extends Resource> extends BaseJob<K> {
    ResourceQueue<String> queueA;
    private final ResourceFetcher<K> fetchResources;
    private static final Logger logger = LogManager.getLogger(ResourceProducer.class);

    public ResourceProducer(ResourceFetcher<K> fetchResources, ResourceQueue<String> queueA) {
        this.fetchResources = fetchResources;
        this.queueA = queueA;
    }

    @Override
    public void _run() throws Exception {
        fetchResources.listResources(resources -> {
            logger.info(String.format("adding %s resources into Queue", resources.size()));
            queueA.addAll(resources);
        });
        logger.info("finish listing resources");
        queueA.setFinishedPopulating(true);
    }
}
