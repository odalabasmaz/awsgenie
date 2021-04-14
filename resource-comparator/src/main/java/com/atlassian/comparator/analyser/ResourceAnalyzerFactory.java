package com.atlassian.comparator.analyser;


import com.atlassian.comparator.ResourceComparator;
import io.awsgenie.fetcher.ResourceFetcher;
import io.awsgenie.fetcher.Service;

import javax.naming.OperationNotSupportedException;

public class ResourceAnalyzerFactory {

    public static BaseResourceAnalyzer getAnalyzer(Service service, ResourceComparator comparator, ResourceFetcher sourceResourceFetcher, ResourceFetcher targetResourceFetcher, long sleepBetweenIterations)
            throws OperationNotSupportedException {

        if (Service.LAMBDA.equals(service)) {
            return new LambdaAnalyzer(comparator, sourceResourceFetcher, targetResourceFetcher, sleepBetweenIterations);
        } else if (Service.CLOUDWATCH.equals(service)) {
            return new CloudWatchAnalyzer(comparator, sourceResourceFetcher, targetResourceFetcher, sleepBetweenIterations);
        } else if (Service.DYNAMODB.equals(service)) {
            return new DynamoAnalyzer(comparator, sourceResourceFetcher, targetResourceFetcher, sleepBetweenIterations);
        } else if (Service.SQS.equals(service)) {
            return new SQSResourceAnalyzer(comparator, sourceResourceFetcher, targetResourceFetcher, sleepBetweenIterations);
        } else if (Service.IAM_ROLE.equals(service)) {
            return new IamRoleAnalyzer(comparator, sourceResourceFetcher, targetResourceFetcher, sleepBetweenIterations);
        } else if (Service.IAM_POLICY.equals(service)) {
            return new IAMPolicyAnalzer(comparator, sourceResourceFetcher, targetResourceFetcher, sleepBetweenIterations);
        } else if (Service.SNS.equals(service)) {
            return new SNSResourceAnalyzer(comparator, sourceResourceFetcher, targetResourceFetcher, sleepBetweenIterations);
        } else if (Service.KINESIS.equals(service)) {
            return new KinesisAnalyzer(comparator, sourceResourceFetcher, targetResourceFetcher, sleepBetweenIterations);
        } else {
            throw new OperationNotSupportedException("Service not supported: " + service);
        }
    }
}
