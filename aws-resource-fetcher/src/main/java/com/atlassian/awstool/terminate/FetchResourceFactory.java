package com.atlassian.awstool.terminate;

import com.atlassian.awstool.terminate.cloudwatch.FetchCloudwatchResources;
import com.atlassian.awstool.terminate.dynamodb.FetchDynamodbResources;
import com.atlassian.awstool.terminate.iamPolicy.FetchIAMPolicyResources;
import com.atlassian.awstool.terminate.iamrole.FetchIAMRoleResources;
import com.atlassian.awstool.terminate.kinesis.FetchKinesisResources;
import com.atlassian.awstool.terminate.lambda.FetchLambdaResources;
import com.atlassian.awstool.terminate.sns.FetchSNSResources;
import com.atlassian.awstool.terminate.sqs.FetchSQSResources;
import credentials.AwsClientConfiguration;

import javax.naming.OperationNotSupportedException;

/**
 * @author Orhun Dalabasmaz
 * @version 10.03.2021
 */

public class FetchResourceFactory<R extends AWSResource> {

    public FetchResources<R> getFetcher(Service service, FetcherConfiguration configuration)
            throws OperationNotSupportedException {

        if (Service.LAMBDA.equals(service)) {
            return (FetchResources<R>) new FetchLambdaResources(configuration);
        } else if (Service.CLOUDWATCH.equals(service)) {
            return (FetchResources<R>) new FetchCloudwatchResources(configuration);
        } else if (Service.DYNAMODB.equals(service)) {
            return (FetchResources<R>) new FetchDynamodbResources(configuration);
        } else if (Service.SQS.equals(service)) {
            return (FetchResources<R>) new FetchSQSResources(configuration);
        } else if (Service.IAM_ROLE.equals(service)) {
            return (FetchResources<R>) new FetchIAMRoleResources(configuration);
        } else if (Service.IAM_POLICY.equals(service)) {
            return (FetchResources<R>) new FetchIAMPolicyResources(configuration);
        } else if (Service.SNS.equals(service)) {
            return (FetchResources<R>) new FetchSNSResources(configuration);
        } else if (Service.KINESIS.equals(service)) {
            return (FetchResources<R>) new FetchKinesisResources(configuration);
        } else {
            throw new OperationNotSupportedException("Service not supported: " + service);
        }
    }
}
