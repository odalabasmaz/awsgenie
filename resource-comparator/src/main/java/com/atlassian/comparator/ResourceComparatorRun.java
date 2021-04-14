package com.atlassian.comparator;

import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import com.atlassian.comparator.analyser.BaseResourceAnalyzer;
import com.atlassian.comparator.analyser.ResourceAnalyzerFactory;
import com.atlassian.comparator.configuration.ParameterConfiguration;
import io.awsgenie.fetcher.ResourceFetcher;
import io.awsgenie.fetcher.ResourceFetcherConfiguration;
import io.awsgenie.fetcher.ResourceFetcherFactory;
import io.awsgenie.fetcher.Service;
import io.awsgenie.fetcher.credentials.AWSClientProvider;
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

        ComparatorExecutor task = new ComparatorExecutor(3, 3);
        task.run(parameterConfiguration);
    }

    public static class ComparatorExecutor extends BaseExecutor {

        public static final int SLEEP_BETWEEN_ITERATIONS = 5000;

        public ComparatorExecutor(int numberOfThreads, int maxQueuesize) {
            super(numberOfThreads, maxQueuesize);
        }

        public void run(ParameterConfiguration configuration) throws Exception {
            ResourceFetcherConfiguration sourceFetcherConfiguration = new ResourceFetcherConfiguration(configuration.getSourceAssumeRoleArn(), configuration.getSourceRegion());
            ResourceFetcher sourceFetcher = new ResourceFetcherFactory<>().getFetcher(
                    Service.fromValue(configuration.getService()), sourceFetcherConfiguration);
            ResourceFetcherConfiguration targetFetchetConfiguration = new ResourceFetcherConfiguration(configuration.getTargetAssumeRoleArn(), configuration.getTargetRegion());
            ResourceFetcher targetFetcher = new ResourceFetcherFactory<>().getFetcher(
                    Service.fromValue(configuration.getService()), targetFetchetConfiguration);

            ResourceQueue<String> sourceQueue = new ResourceQueue<>();
            ResourceQueue<String> targetQueue = new ResourceQueue<>();
            ResourceQueue<String> commonQueue = new ResourceQueue<>();

            ResourceProducer resourceProducerA = new ResourceProducer(sourceFetcher, sourceQueue);
            ResourceProducer resourceProducerB = new ResourceProducer(targetFetcher, targetQueue);
            ResourceComparator resourceComparator = new ResourceComparator(sourceQueue, targetQueue, commonQueue, SLEEP_BETWEEN_ITERATIONS);
            BaseResourceAnalyzer resourceAnalyzer = ResourceAnalyzerFactory.getAnalyzer(Service.fromValue(configuration.getService()),
                    resourceComparator, sourceFetcher, targetFetcher, SLEEP_BETWEEN_ITERATIONS);


            String sourceAccountId = AWSClientProvider.getInstance(sourceFetcherConfiguration).getAmazonSts().getCallerIdentity(new GetCallerIdentityRequest()).getAccount();
            String targetAccountId = AWSClientProvider.getInstance(targetFetchetConfiguration).getAmazonSts().getCallerIdentity(new GetCallerIdentityRequest()).getAccount();
            BaseResourceAnalyzer.putIntoReplacer(sourceAccountId, "ACCOUNT_ID");
            BaseResourceAnalyzer.putIntoReplacer(targetAccountId, "ACCOUNT_ID");
            BaseResourceAnalyzer.putIntoReplacer(configuration.getSourceRegion(), "REGION");
            BaseResourceAnalyzer.putIntoReplacer(configuration.getTargetRegion(), "REGION");


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
