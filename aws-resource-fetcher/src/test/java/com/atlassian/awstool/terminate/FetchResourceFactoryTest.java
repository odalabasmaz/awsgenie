package com.atlassian.awstool.terminate;

import com.atlassian.awstool.terminate.cloudwatch.CloudwatchResource;
import com.atlassian.awstool.terminate.cloudwatch.FetchCloudwatchResources;
import com.atlassian.awstool.terminate.dynamodb.DynamoDBResource;
import com.atlassian.awstool.terminate.dynamodb.FetchDynamoDBResources;
import com.atlassian.awstool.terminate.iam.FetchIAMPolicyResources;
import com.atlassian.awstool.terminate.iam.FetchIAMRoleResources;
import com.atlassian.awstool.terminate.iam.IAMPolicyResource;
import com.atlassian.awstool.terminate.iam.IAMRoleResource;
import com.atlassian.awstool.terminate.kinesis.FetchKinesisResources;
import com.atlassian.awstool.terminate.kinesis.KinesisResource;
import com.atlassian.awstool.terminate.lambda.FetchLambdaResources;
import com.atlassian.awstool.terminate.lambda.LambdaResource;
import com.atlassian.awstool.terminate.sns.FetchSNSResources;
import com.atlassian.awstool.terminate.sns.SNSResource;
import com.atlassian.awstool.terminate.sqs.FetchSQSResources;
import com.atlassian.awstool.terminate.sqs.SQSResource;
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
public class FetchResourceFactoryTest {
    @Mock
    private FetcherConfiguration fetcherConfiguration;

    private FetchResourceFactory fetchResourceFactory;

    @Before
    public void setUp() throws Exception {
        fetchResourceFactory = new FetchResourceFactory();
    }

    @Test
    public void getFetcher() throws OperationNotSupportedException {
        FetchResources<LambdaResource> lambdaFetcher = fetchResourceFactory.getFetcher(Service.LAMBDA, fetcherConfiguration);
        assertThat(lambdaFetcher, is(instanceOf(FetchLambdaResources.class)));
        FetchResources<CloudwatchResource> cloudwatchFetcher = fetchResourceFactory.getFetcher(Service.CLOUDWATCH, fetcherConfiguration);
        assertThat(cloudwatchFetcher, is(instanceOf(FetchCloudwatchResources.class)));
        FetchResources<DynamoDBResource> dynamoDBFetcher = fetchResourceFactory.getFetcher(Service.DYNAMODB, fetcherConfiguration);
        assertThat(dynamoDBFetcher, is(instanceOf(FetchDynamoDBResources.class)));
        FetchResources<SQSResource> sqsFetcher = fetchResourceFactory.getFetcher(Service.SQS, fetcherConfiguration);
        assertThat(sqsFetcher, is(instanceOf(FetchSQSResources.class)));
        FetchResources<IAMRoleResource> iamRoleFetcher = fetchResourceFactory.getFetcher(Service.IAM_ROLE, fetcherConfiguration);
        assertThat(iamRoleFetcher, is(instanceOf(FetchIAMRoleResources.class)));
        FetchResources<IAMPolicyResource> iamPolicyFetcher = fetchResourceFactory.getFetcher(Service.IAM_POLICY, fetcherConfiguration);
        assertThat(iamPolicyFetcher, is(instanceOf(FetchIAMPolicyResources.class)));
        FetchResources<SNSResource> snsFetcher = fetchResourceFactory.getFetcher(Service.SNS, fetcherConfiguration);
        assertThat(snsFetcher, is(instanceOf(FetchSNSResources.class)));
        FetchResources<KinesisResource> kinesisFetcher = fetchResourceFactory.getFetcher(Service.KINESIS, fetcherConfiguration);
        assertThat(kinesisFetcher, is(instanceOf(FetchKinesisResources.class)));
    }

    @Test(expected = OperationNotSupportedException.class)
    public void getFetcherWithInvalidService() throws Exception {
        fetchResourceFactory.getFetcher(Service.CLOUDFRONT, fetcherConfiguration);
    }
}