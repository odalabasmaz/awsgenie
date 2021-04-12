package com.atlassian.comparator;

import com.atlassian.awstool.terminate.FetchResourceFactory;
import com.atlassian.awstool.terminate.FetchResources;
import com.atlassian.awstool.terminate.FetcherConfiguration;
import com.atlassian.awstool.terminate.Service;
import com.atlassian.comparator.configuration.ParameterConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.naming.OperationNotSupportedException;
import java.util.Arrays;

public class ResourceComparatorRun {
    private static final Logger LOGGER = LogManager.getLogger(ResourceComparatorRun.class);

    public static void main(String[] args) throws InterruptedException, OperationNotSupportedException {
        ParameterConfiguration parameterConfiguration = new ParameterConfiguration();

        try {
            parameterConfiguration.fromSystemArgs(args);
        } catch (Exception e) {
            throw new IllegalArgumentException("Can not convert arguments " + Arrays.toString(args) + " to "
                    + ParameterConfiguration.class.getSimpleName() + " Reason " + e, e);
        }

        ResourceComparatorRun task = new ResourceComparatorRun();
        task.run(parameterConfiguration);
    }

    public void run(ParameterConfiguration configuration) throws InterruptedException, OperationNotSupportedException {
        ResourceQueue queueOne = new ResourceQueue();
        ResourceQueue queueTwo = new ResourceQueue();

        FetchResources fetcher = new FetchResourceFactory<>().getFetcher(
                Service.fromValue(configuration.getService()),
                new FetcherConfiguration(configuration.getAssumeRoleArn(), configuration.getRegion())
        );

        ResourceProducer resourceProducerA = new ResourceProducer(fetcher, queueOne);
        ResourceProducer resourceProducerB = new ResourceProducer(fetcher, queueTwo);
        ResourceAnalyzer resourceAnalyzer = new ResourceAnalyzer();

        ResourceComparator resourceComparator = new ResourceComparator(resourceProducerA, resourceProducerB, resourceAnalyzer);
        resourceComparator.run();
    }
}
