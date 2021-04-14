package com.atlassian.awsgenie.fetcher;

import com.atlassian.awsgenie.fetcher.cloudwatch.CloudWatchResource;
import com.atlassian.awsgenie.fetcher.cloudwatch.CloudWatchResourceFetcher;
import com.atlassian.awsgenie.fetcher.dynamodb.DynamoDBResource;
import com.atlassian.awsgenie.fetcher.dynamodb.DynamoDBResourceFetcher;
import com.atlassian.awsgenie.fetcher.iam.IAMPolicyResource;
import com.atlassian.awsgenie.fetcher.iam.IAMPolicyResourceFetcher;
import com.atlassian.awsgenie.fetcher.iam.IAMRoleResource;
import com.atlassian.awsgenie.fetcher.iam.IAMRoleResourceFetcher;
import com.atlassian.awsgenie.fetcher.kinesis.KinesisResource;
import com.atlassian.awsgenie.fetcher.kinesis.KinesisResourceFetcher;
import com.atlassian.awsgenie.fetcher.lambda.LambdaResource;
import com.atlassian.awsgenie.fetcher.lambda.LambdaResourceFetcher;
import com.atlassian.awsgenie.fetcher.sns.SNSResource;
import com.atlassian.awsgenie.fetcher.sns.SNSResourceFetcher;
import com.atlassian.awsgenie.fetcher.sqs.SQSResource;
import com.atlassian.awsgenie.fetcher.sqs.SQSResourceFetcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.naming.OperationNotSupportedException;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Celal Emre CICEK
 * @version 13.04.2021
 */

@RunWith(MockitoJUnitRunner.class)
public class ResourceFetcherFactoryTest {
    @Mock
    private ResourceFetcherConfiguration resourceFetcherConfiguration;

    private ResourceFetcherFactory resourceFetcherFactory;

    @Before
    public void setUp() throws Exception {
        resourceFetcherFactory = new ResourceFetcherFactory();
    }

    @Test
    public void getFetcher() throws OperationNotSupportedException {
        ResourceFetcher<LambdaResource> lambdaFetcher = resourceFetcherFactory.getFetcher(Service.LAMBDA, resourceFetcherConfiguration);
        assertThat(lambdaFetcher, is(instanceOf(LambdaResourceFetcher.class)));
        ResourceFetcher<CloudWatchResource> cloudwatchFetcher = resourceFetcherFactory.getFetcher(Service.CLOUDWATCH, resourceFetcherConfiguration);
        assertThat(cloudwatchFetcher, is(instanceOf(CloudWatchResourceFetcher.class)));
        ResourceFetcher<DynamoDBResource> dynamoDBFetcher = resourceFetcherFactory.getFetcher(Service.DYNAMODB, resourceFetcherConfiguration);
        assertThat(dynamoDBFetcher, is(instanceOf(DynamoDBResourceFetcher.class)));
        ResourceFetcher<SQSResource> sqsFetcher = resourceFetcherFactory.getFetcher(Service.SQS, resourceFetcherConfiguration);
        assertThat(sqsFetcher, is(instanceOf(SQSResourceFetcher.class)));
        ResourceFetcher<IAMRoleResource> iamRoleFetcher = resourceFetcherFactory.getFetcher(Service.IAM_ROLE, resourceFetcherConfiguration);
        assertThat(iamRoleFetcher, is(instanceOf(IAMRoleResourceFetcher.class)));
        ResourceFetcher<IAMPolicyResource> iamPolicyFetcher = resourceFetcherFactory.getFetcher(Service.IAM_POLICY, resourceFetcherConfiguration);
        assertThat(iamPolicyFetcher, is(instanceOf(IAMPolicyResourceFetcher.class)));
        ResourceFetcher<SNSResource> snsFetcher = resourceFetcherFactory.getFetcher(Service.SNS, resourceFetcherConfiguration);
        assertThat(snsFetcher, is(instanceOf(SNSResourceFetcher.class)));
        ResourceFetcher<KinesisResource> kinesisFetcher = resourceFetcherFactory.getFetcher(Service.KINESIS, resourceFetcherConfiguration);
        assertThat(kinesisFetcher, is(instanceOf(KinesisResourceFetcher.class)));
    }

    @Test(expected = OperationNotSupportedException.class)
    public void getFetcherWithInvalidService() throws Exception {
        resourceFetcherFactory.getFetcher(Service.CLOUDFRONT, resourceFetcherConfiguration);
    }
}