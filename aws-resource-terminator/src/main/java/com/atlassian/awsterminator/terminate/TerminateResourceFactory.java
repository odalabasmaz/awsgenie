package com.atlassian.awsterminator.terminate;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.atlassian.awsterminator.configuration.Configuration;
import com.atlassian.awstool.terminate.Service;
import credentials.AwsClientConfiguration;

import javax.naming.OperationNotSupportedException;

/**
 * @author Orhun Dalabasmaz
 * @version 10.03.2021
 */

public class TerminateResourceFactory {
    public TerminateResources getTerminator(Service service, AwsClientConfiguration configuration) throws OperationNotSupportedException {
        if (Service.SQS.equals(service)) {
            return new TerminateSqsResources(configuration);
        } else if (Service.CLOUDWATCH.equals(service)) {
            return new TerminateCloudwatchResources(configuration);
        } else if (Service.LAMBDA.equals(service)) {
            return new TerminateLambdaResources(configuration);
        } else if (Service.DYNAMODB.equals(service)) {
            return new TerminateDynamoDBResources(configuration);
        } else if (Service.SNS.equals(service)) {
            return new TerminateSnsResources(configuration);
        } else if (Service.IAM_ROLE.equals(service)) {
            return new TerminateIamRoleResources(configuration);
        } else if (Service.IAM_POLICY.equals(service)) {
            return new TerminateIamPolicyResources(configuration);
        } else if (Service.KINESIS.equals(service)) {
            return new TerminateKinesisResources(configuration);
        } else {
            throw new OperationNotSupportedException("Service not supported: " + service);
        }
    }
}
