package com.atlassian.awsterminator.terminate;

import com.amazonaws.auth.AWSCredentialsProvider;

import javax.naming.OperationNotSupportedException;

/**
 * @author Orhun Dalabasmaz
 * @version 10.03.2021
 */

public class TerminateResourceFactory {
    public TerminateResources getTerminator(String service, AWSCredentialsProvider credentialsProvider) throws OperationNotSupportedException {
        if ("sqs".equalsIgnoreCase(service)) {
            return new TerminateSqsResources(credentialsProvider);
        } else if ("cloudwatch".equalsIgnoreCase(service)) {
            return new TerminateCloudwatchResources(credentialsProvider);
        } else if ("lambda".equalsIgnoreCase(service)) {
            return new TerminateLambdaResources(credentialsProvider);
        } else if ("dynamodb".equalsIgnoreCase(service)) {
            return new TerminateDynamoDBResources(credentialsProvider);
        } else if ("role".equalsIgnoreCase(service)) {
            return new TerminateIamRoleResources(credentialsProvider);
        } else if ("policy".equalsIgnoreCase(service)) {
            return new TerminateIamPolicyResources(credentialsProvider);
        } else if ("sns".equalsIgnoreCase(service)) {
            return new TerminateSnsResources(credentialsProvider);
        } else {
            throw new OperationNotSupportedException("Service not supported: " + service);
        }
    }
}
