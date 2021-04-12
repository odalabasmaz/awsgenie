package com.atlassian.comparator;

import com.atlassian.comparator.configuration.ParameterConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;

public class ResourceComparatorRun {
    private static final Logger LOGGER = LogManager.getLogger(ResourceComparatorRun.class);

    public static void main(String[] args) {
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

    public void run(ParameterConfiguration configuration) {
        ResourceQueue queueOne = new ResourceQueue();
        ResourceQueue queueTwo = new ResourceQueue();

        ResourceProducer resourceProducerA = new ResourceProducer(queueOne);
        ResourceProducer resourceProducerB = new ResourceProducer(queueTwo);
        ResourceAnalyzer resourceAnalyzer = new ResourceAnalyzer();

        ResourceComparator resourceComparator = new ResourceComparator(resourceProducerA, resourceProducerB, resourceAnalyzer);
        resourceComparator.run();
    }
}
