package io.github.odalabasmaz.awsgenie.fetcher;

import io.github.odalabasmaz.awsgenie.fetcher.cloudwatch.CloudWatchResourceFetcher;
import io.github.odalabasmaz.awsgenie.fetcher.dynamodb.DynamoDBResourceFetcher;
import io.github.odalabasmaz.awsgenie.fetcher.iam.IAMPolicyResourceFetcher;
import io.github.odalabasmaz.awsgenie.fetcher.iam.IAMRoleResourceFetcher;
import io.github.odalabasmaz.awsgenie.fetcher.kinesis.KinesisResourceFetcher;
import io.github.odalabasmaz.awsgenie.fetcher.lambda.LambdaResourceFetcher;
import io.github.odalabasmaz.awsgenie.fetcher.sns.SNSResourceFetcher;
import io.github.odalabasmaz.awsgenie.fetcher.sqs.SQSResourceFetcher;

import javax.naming.OperationNotSupportedException;

/**
 * @author Orhun Dalabasmaz
 * @version 10.03.2021
 */

public class ResourceFetcherFactory<R extends Resource> {

    public ResourceFetcher<R> getFetcher(Service service, ResourceFetcherConfiguration configuration)
            throws OperationNotSupportedException {

        if (Service.LAMBDA.equals(service)) {
            return (ResourceFetcher<R>) new LambdaResourceFetcher(configuration);
        } else if (Service.CLOUDWATCH.equals(service)) {
            return (ResourceFetcher<R>) new CloudWatchResourceFetcher(configuration);
        } else if (Service.DYNAMODB.equals(service)) {
            return (ResourceFetcher<R>) new DynamoDBResourceFetcher(configuration);
        } else if (Service.SQS.equals(service)) {
            return (ResourceFetcher<R>) new SQSResourceFetcher(configuration);
        } else if (Service.IAM_ROLE.equals(service)) {
            return (ResourceFetcher<R>) new IAMRoleResourceFetcher(configuration);
        } else if (Service.IAM_POLICY.equals(service)) {
            return (ResourceFetcher<R>) new IAMPolicyResourceFetcher(configuration);
        } else if (Service.SNS.equals(service)) {
            return (ResourceFetcher<R>) new SNSResourceFetcher(configuration);
        } else if (Service.KINESIS.equals(service)) {
            return (ResourceFetcher<R>) new KinesisResourceFetcher(configuration);
        } else {
            throw new OperationNotSupportedException("Service not supported: " + service);
        }
    }
}
