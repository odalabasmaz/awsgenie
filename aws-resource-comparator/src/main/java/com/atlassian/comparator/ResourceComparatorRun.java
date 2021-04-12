package com.atlassian.comparator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * @author Orhun Dalabasmaz
 * @version 10.03.2021
 */

public class ResourceComparatorRun {
    private static final Logger LOGGER = LogManager.getLogger(ResourceComparatorRun.class);
    private static final String STS_SESSION_NAME_PREFIX = "aws_resource_comparator_";
    private static final String DEFAULT_CONFIG_FILE_PATH = System.getProperty("user.home") + "/.awsterminator/config.json";

    public static void main(String[] args) throws Exception {



        ResourceProducer resourceProducerA = new ResourceProducer(new ResourceQueue());
        ResourceProducer resourceProducerB = new ResourceProducer(new ResourceQueue());

        ResourceComparator resourceComparator = new ResourceComparator(resourceProducerA,resourceProducerB);
        ResourceAnalyzer resourceAnalyzer = new ResourceAnalyzer();

    }


}
