package com.atlassian.awsgenie.terminator;

import com.atlassian.awsgenie.fetcher.Service;
import com.atlassian.awsgenie.fetcher.credentials.AWSClientConfiguration;

import javax.naming.OperationNotSupportedException;

/**
 * @author Orhun Dalabasmaz
 * @version 10.03.2021
 */

public class ResourceTerminatorFactory {
    public ResourceTerminator getTerminator(Service service, AWSClientConfiguration configuration) throws OperationNotSupportedException {
        if (Service.SQS.equals(service)) {
            return new SQSResourceTerminator(configuration);
        } else if (Service.CLOUDWATCH.equals(service)) {
            return new CloudWatchResourceTerminator(configuration);
        } else if (Service.LAMBDA.equals(service)) {
            return new LambdaResourceTerminator(configuration);
        } else if (Service.DYNAMODB.equals(service)) {
            return new DynamoDBResourceTerminator(configuration);
        } else if (Service.SNS.equals(service)) {
            return new SNSResourceTerminator(configuration);
        } else if (Service.IAM_ROLE.equals(service)) {
            return new IAMRoleResourceTerminator(configuration);
        } else if (Service.IAM_POLICY.equals(service)) {
            return new IAMPolicyResourceTerminator(configuration);
        } else if (Service.KINESIS.equals(service)) {
            return new KinesisResourceTerminator(configuration);
        } else {
            throw new OperationNotSupportedException("Service not supported: " + service);
        }
    }
}
