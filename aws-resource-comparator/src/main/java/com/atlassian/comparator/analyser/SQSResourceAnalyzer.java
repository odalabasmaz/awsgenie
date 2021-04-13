package com.atlassian.comparator.analyser;

import com.atlassian.awstool.terminate.FetchResources;
import com.atlassian.awstool.terminate.sqs.SQSResource;
import com.atlassian.comparator.ResourceComparator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class SQSResourceAnalyzer extends BaseResourceAnalyzer {
    private static final Logger LOGGER = LogManager.getLogger(SQSResourceAnalyzer.class);

    private final List<String> SQS_ATTRIBUTES = Arrays.asList(
            "VisibilityTimeout",
            "DelaySeconds",
            "MessageRetentionPeriod"
    );

    public SQSResourceAnalyzer(ResourceComparator comparator, FetchResources sourceResourceFetcher, FetchResources targetResourceFetcher, long sleepBetweenIterations) {
        super(comparator, sourceResourceFetcher, targetResourceFetcher, sleepBetweenIterations);
    }

    @Override
    void processCommonQueue() throws Exception {
        List<String> sqsList = comparator.getCommonQueue().getAll();
        List<String> details = new LinkedList<>();
        for (String queueName : sqsList) {
            LOGGER.info(String.format(">>>>>Comparing queue: %s<<<<<", queueName));

            SQSResource sourceAccountSQSResource = (SQSResource) sourceResourceFetcher.fetchResources(Collections.singletonList(queueName), details).get(0);
            SQSResource targetAccountSQSResource = (SQSResource) targetResourceFetcher.fetchResources(Collections.singletonList(queueName), details).get(0);
            Map<String, String> sourceQueueAttributeMap = (Map<String, String>) sourceAccountSQSResource.getResourceObject();
            Map<String, String> targetQueueAttributeMap = (Map<String, String>) targetAccountSQSResource.getResourceObject();
            for (String attributeKey : SQS_ATTRIBUTES) {
                if (!sourceQueueAttributeMap.get(attributeKey).equals(targetQueueAttributeMap.get(attributeKey))) {
                    LOGGER.info(String.format("Difference in %s, source value: %s, target value: %s", attributeKey,
                            sourceQueueAttributeMap.get(attributeKey), targetQueueAttributeMap.get(attributeKey)));
                }
            }

            if (sourceAccountSQSResource.getLambdaTriggers().size() != targetAccountSQSResource.getLambdaTriggers().size()) {
                LOGGER.info("Difference in lambda triggers!");
                LOGGER.info(String.format("Source queue triggers %s", sourceAccountSQSResource.getLambdaTriggers().size()));
                LOGGER.info(String.format("Target queue triggers %s", targetAccountSQSResource.getLambdaTriggers().size()));
            }

            if (sourceAccountSQSResource.getSnsSubscriptions().size() != targetAccountSQSResource.getSnsSubscriptions().size()) {
                LOGGER.info("Difference in sns subscriptions!");
                LOGGER.info(String.format("Source queue subscriptions %s", sourceAccountSQSResource.getSnsSubscriptions().size()));
                LOGGER.info(String.format("Target queue subscriptions %s", targetAccountSQSResource.getSnsSubscriptions().size()));
            }

            if (!sourceAccountSQSResource.getCloudwatchAlarms().equals(targetAccountSQSResource.getCloudwatchAlarms())) {
                LOGGER.info("Difference in cloudwatch alarms!");
                LOGGER.info(String.format("Source queue alarms %s", sourceAccountSQSResource.getCloudwatchAlarms()));
                LOGGER.info(String.format("Target queue alarms %s", targetAccountSQSResource.getCloudwatchAlarms()));
            }
        }
    }
}