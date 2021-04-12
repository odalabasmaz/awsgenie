package com.atlassian.awsterminator.terminate;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.atlassian.awstool.terminate.Service;
import com.atlassian.awstool.terminate.cloudwatch.CloudwatchResource;
import com.atlassian.awstool.terminate.dynamodb.DynamodbResource;
import com.atlassian.awstool.terminate.iamPolicy.IAMPolicyResource;
import com.atlassian.awstool.terminate.iamrole.IAMRoleResource;
import com.atlassian.awstool.terminate.kinesis.KinesisResource;
import com.atlassian.awstool.terminate.lambda.LambdaResource;
import com.atlassian.awstool.terminate.sns.SNSResource;
import com.atlassian.awstool.terminate.sqs.SQSResource;
import credentials.AwsClientConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.naming.OperationNotSupportedException;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;


/**
 * @author Celal Emre CICEK
 * @version 9.04.2021
 */

@RunWith(MockitoJUnitRunner.class)
public class TerminateResourceFactoryTest {
    @Mock
    private AWSCredentialsProvider credentialsProvider;

    private TerminateResourceFactory terminateResourceFactory;

    @Before
    public void setUp() throws Exception {
        terminateResourceFactory = new TerminateResourceFactory();
    }

    @Test
    public void getTerminator() throws Exception {
        AwsClientConfiguration configuration = TerminatorHelper.getRegion1Account1Configuration();
        TerminateResources<SQSResource> sqs = terminateResourceFactory.getTerminator(Service.SQS, configuration);
        assertThat(sqs, is(instanceOf(TerminateSqsResources.class)));
        TerminateResources<CloudwatchResource> cloudwatch = terminateResourceFactory.getTerminator(Service.CLOUDWATCH, configuration);
        assertThat(cloudwatch, is(instanceOf(TerminateCloudwatchResources.class)));
        TerminateResources<LambdaResource> lambda = terminateResourceFactory.getTerminator(Service.LAMBDA, configuration);
        assertThat(lambda, is(instanceOf(TerminateLambdaResources.class)));
        TerminateResources<DynamodbResource> dynamodb = terminateResourceFactory.getTerminator(Service.DYNAMODB, configuration);
        assertThat(dynamodb, is(instanceOf(TerminateDynamoDBResources.class)));
        TerminateResources<SNSResource> sns = terminateResourceFactory.getTerminator(Service.SNS, configuration);
        assertThat(sns, is(instanceOf(TerminateSnsResources.class)));
        TerminateResources<IAMRoleResource> iamRole = terminateResourceFactory.getTerminator(Service.IAM_ROLE, configuration);
        assertThat(iamRole, is(instanceOf(TerminateIamRoleResources.class)));
        TerminateResources<IAMPolicyResource> iamPolicy = terminateResourceFactory.getTerminator(Service.IAM_POLICY, configuration);
        assertThat(iamPolicy, is(instanceOf(TerminateIamPolicyResources.class)));
        TerminateResources<KinesisResource> kinesis = terminateResourceFactory.getTerminator(Service.KINESIS, configuration);
        assertThat(kinesis, is(instanceOf(TerminateKinesisResources.class)));
    }

    @Test(expected = OperationNotSupportedException.class)
    public void getTerminatorWithInvalidService() throws Exception {
        terminateResourceFactory.getTerminator(Service.CLOUDFRONT, TerminatorHelper.getRegion1Account1Configuration());
    }
}