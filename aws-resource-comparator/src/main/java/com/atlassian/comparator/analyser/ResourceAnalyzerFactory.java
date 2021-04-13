package com.atlassian.comparator.analyser;

import com.atlassian.awstool.terminate.FetchResources;
import com.atlassian.awstool.terminate.Service;
import com.atlassian.comparator.ResourceComparator;

import javax.naming.OperationNotSupportedException;

public class ResourceAnalyzerFactory {

    public static BaseResourceAnalyzer getAnalyzer(Service service, ResourceComparator comparator, FetchResources sourceResourceFetcher, FetchResources targetResourceFetcher, long sleepBetweenIterations)
            throws OperationNotSupportedException {

        if (Service.SQS.equals(service)) {
            return new SQSResourceAnalyzer(comparator, sourceResourceFetcher, targetResourceFetcher, sleepBetweenIterations);
        } else {
            throw new OperationNotSupportedException("Service not supported: " + service);
        }
    }
}
