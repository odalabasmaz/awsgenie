package com.atlassian.awsterminator.terminate;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.atlassian.awstool.terminate.Service;

import javax.naming.OperationNotSupportedException;

/**
 * @author Orhun Dalabasmaz
 * @version 10.03.2021
 */

public class TerminateResourceFactory {
    public TerminateResources getTerminator(Service service, AWSCredentialsProvider credentialsProvider) throws OperationNotSupportedException {
        if (Service.SQS.equals(service)) {
            return new TerminateSqsResources(credentialsProvider);
        } else if (Service.CLOUDWATCH.equals(service)) {
            return new TerminateCloudwatchResources(credentialsProvider);
        } else if (Service.LAMBDA.equals(service)) {
            return new TerminateLambdaResources(credentialsProvider);
        } else if (Service.DYNAMODB.equals(service)) {
            return new TerminateDynamoDBResources(credentialsProvider);
        } else if (Service.SNS.equals(service)) {
            return new TerminateSnsResources(credentialsProvider);
        } else if (Service.IAM_ROLE.equals(service)) {
            return new TerminateIamRoleResources(credentialsProvider);
        } else if (Service.IAM_POLICY.equals(service)) {
            return new TerminateIamPolicyResources(credentialsProvider);
        } else if (Service.KINESIS.equals(service)) {
            return new TerminateKinesisResources(credentialsProvider);
        } else {
            throw new OperationNotSupportedException("Service not supported: " + service);
        }
    }
}
