package com.atlassian.comparator.analyser;

import com.atlassian.comparator.ResourceComparator;
import com.atlassian.comparator.ResourceQueue;
import com.atlassian.comparator.analyser.diff.generator.BaseDiffGenerator;
import io.awsgenie.fetcher.Resource;
import io.awsgenie.fetcher.ResourceFetcher;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public abstract class BaseResourceAnalyzer<T extends Resource> {
    private final static Map<String, String> replacer = new HashMap<>();
    protected final ResourceComparator comparator;
    protected final ResourceFetcher<T> sourceResourceFetcher;
    protected final ResourceFetcher<T> targetResourceFetcher;
    protected final long sleepBetweenIterations;
    private static final Logger LOGGER = LogManager.getLogger(BaseResourceAnalyzer.class);

    public BaseResourceAnalyzer(ResourceComparator comparator,
                                ResourceFetcher<T> sourceResourceFetcher,
                                ResourceFetcher<T> targetResourceFetcher,
                                long sleepBetweenIterations) {
        this.comparator = comparator;
        this.sourceResourceFetcher = sourceResourceFetcher;
        this.targetResourceFetcher = targetResourceFetcher;
        this.sleepBetweenIterations = sleepBetweenIterations;
    }

    public void run() throws Exception {
        ResourceQueue commonQueue = comparator.getCommonQueue();
        while (!commonQueue.isFinishedPopulating()) {
            List<String> sqsList = commonQueue.getAll();
            List<String> details = new LinkedList<>();
            for (String resourceName : sqsList) {
                StringBuilder builder = new StringBuilder();
                builder.append("resourceName :").append(resourceName);
                T sourceAccountSQSResource = (T) sourceResourceFetcher.fetchResources(Collections.singletonList(resourceName), details).get(0);
                T targetAccountSQSResource = (T) targetResourceFetcher.fetchResources(Collections.singletonList(resourceName), details).get(0);
                replace(sourceAccountSQSResource);
                replace(targetAccountSQSResource);
                BaseDiffGenerator baseDiffGenerator = BaseDiffGenerator.get();
                baseDiffGenerator.process(sourceAccountSQSResource,targetAccountSQSResource,builder);
                LOGGER.info(BaseDiffGenerator.get().generate());

            }
            commonQueue.waitNewMessage(sleepBetweenIterations);
        }
        LOGGER.info(BaseDiffGenerator.get().generate());
    }

    protected abstract void replace(T awsResource);

    public static void putIntoReplacer(String key, String value) {
        replacer.put(key, value);
    }

    protected String replaceKeyword(String resource) {
        String value = resource;
        for (Map.Entry<String, String> key : replacer.entrySet()) {
            value = value.replace(key.getKey(), key.getValue());
        }
        return value;
    }
}