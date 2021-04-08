package com.atlassian.awstool.terminate;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.atlassian.awstool.terminate.cloudwatch.FetchCloudwatchResources;
import com.atlassian.awstool.terminate.dynamodb.FetchDynamodbResources;
import com.atlassian.awstool.terminate.iamPolicy.FetchIAMPolicyResources;
import com.atlassian.awstool.terminate.iamrole.FetchIAMRoleResources;
import com.atlassian.awstool.terminate.kinesis.FetchKinesisResources;
import com.atlassian.awstool.terminate.lambda.FetchLambdaResources;
import com.atlassian.awstool.terminate.sns.FetchSNSResources;
import com.atlassian.awstool.terminate.sqs.FetchSQSResources;

import javax.naming.OperationNotSupportedException;

/**
 * @author Orhun Dalabasmaz
 * @version 10.03.2021
 */

public class FetchResourceFactory {
    public FetchResources getFetcher(String service, AWSCredentialsProvider credentialsProvider) throws OperationNotSupportedException {
        if ("lambda".equalsIgnoreCase(service)) {
            return new FetchLambdaResources(credentialsProvider);
        } else if ("cloudwatch".equalsIgnoreCase(service)) {
            return new FetchCloudwatchResources(credentialsProvider);
        } else if ("dynamodb".equalsIgnoreCase(service)) {
            return new FetchDynamodbResources(credentialsProvider);
        } else if ("sqs".equalsIgnoreCase(service)) {
            return new FetchSQSResources(credentialsProvider);
        } else if ("iamRole".equalsIgnoreCase(service)) {
            return new FetchIAMRoleResources(credentialsProvider);
        } else if ("iamPolicy".equalsIgnoreCase(service)) {
            return new FetchIAMPolicyResources(credentialsProvider);
        } else if ("sns".equalsIgnoreCase(service)) {
            return new FetchSNSResources(credentialsProvider);
        } else if ("kinesis".equalsIgnoreCase(service)) {
            return new FetchKinesisResources(credentialsProvider);
        } else {
            throw new OperationNotSupportedException("Service not supported: " + service);
        }
    }
}
