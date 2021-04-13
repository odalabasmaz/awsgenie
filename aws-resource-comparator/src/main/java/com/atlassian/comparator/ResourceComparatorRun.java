package com.atlassian.comparator;

import com.atlassian.awstool.terminate.FetchResourceFactory;
import com.atlassian.awstool.terminate.FetchResources;
import com.atlassian.awstool.terminate.FetcherConfiguration;
import com.atlassian.awstool.terminate.Service;
import com.atlassian.comparator.configuration.ParameterConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;

public class ResourceComparatorRun {
    private static final Logger LOGGER = LogManager.getLogger(ResourceComparatorRun.class);

    public static void main(String[] args) throws Exception {
        ParameterConfiguration parameterConfiguration = new ParameterConfiguration();

        try {
            parameterConfiguration.fromSystemArgs(args);
        } catch (Exception e) {
            throw new IllegalArgumentException("Can not convert arguments " + Arrays.toString(args) + " to "
                    + ParameterConfiguration.class.getSimpleName() + " Reason " + e, e);
        }

        ComparatorExecutor task = new ComparatorExecutor(2, 2);
        task.run(parameterConfiguration);
    }

    public static class ComparatorExecutor extends BaseExecutor {

        public static final int SLEEP_BETWEEN_ITERATIONS = 5000;

        public ComparatorExecutor(int numberOfThreads, int maxQueuesize) {
            super(numberOfThreads, maxQueuesize);
        }

        public void run(ParameterConfiguration configuration) throws Exception {
            FetchResources sourceFetcher = new FetchResourceFactory<>().getFetcher(
                    Service.fromValue(configuration.getService()),
                    new FetcherConfiguration(configuration.getSourceAssumeRoleArn(), configuration.getSourceRegion())
            );
            FetchResources targetFetcher = new FetchResourceFactory<>().getFetcher(
                    Service.fromValue(configuration.getService()),
                    new FetcherConfiguration(configuration.getTargetAssumeRoleArn(), configuration.getTargetRegion())
            );

            ResourceQueue<String> sourceQueue = new ResourceQueue();
            ResourceQueue<String> targetQueue = new ResourceQueue();
            ResourceQueue<String> commonQueue = new ResourceQueue();

            ResourceProducer resourceProducerA = new ResourceProducer(sourceFetcher, sourceQueue);
            ResourceProducer resourceProducerB = new ResourceProducer(targetFetcher, targetQueue);
            ResourceComparator resourceComparator = new ResourceComparator(sourceQueue, targetQueue, commonQueue, SLEEP_BETWEEN_ITERATIONS);
            ResourceAnalyzer resourceAnalyzer = new ResourceAnalyzer(resourceComparator, sourceFetcher, targetFetcher, SLEEP_BETWEEN_ITERATIONS);

            //resourceProducerA.run();
            runJob(resourceProducerA);
            //resourceProducerB.run();
            runJob(resourceProducerB);
            //resourceComparator.run();
            runJob(resourceComparator);

            resourceAnalyzer.run();
            waitForTasks();
        }
    }


}
