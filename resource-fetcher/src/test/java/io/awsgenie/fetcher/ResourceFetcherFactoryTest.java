package io.awsgenie.fetcher;

import io.awsgenie.fetcher.cloudwatch.CloudWatchResource;
import io.awsgenie.fetcher.cloudwatch.CloudWatchResourceFetcher;
import io.awsgenie.fetcher.dynamodb.DynamoDBResource;
import io.awsgenie.fetcher.dynamodb.DynamoDBResourceFetcher;
import io.awsgenie.fetcher.iam.IAMPolicyResource;
import io.awsgenie.fetcher.iam.IAMPolicyResourceFetcher;
import io.awsgenie.fetcher.iam.IAMRoleResource;
import io.awsgenie.fetcher.iam.IAMRoleResourceFetcher;
import io.awsgenie.fetcher.kinesis.KinesisResource;
import io.awsgenie.fetcher.kinesis.KinesisResourceFetcher;
import io.awsgenie.fetcher.lambda.LambdaResource;
import io.awsgenie.fetcher.lambda.LambdaResourceFetcher;
import io.awsgenie.fetcher.sns.SNSResource;
import io.awsgenie.fetcher.sns.SNSResourceFetcher;
import io.awsgenie.fetcher.sqs.SQSResource;
import io.awsgenie.fetcher.sqs.SQSResourceFetcher;
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