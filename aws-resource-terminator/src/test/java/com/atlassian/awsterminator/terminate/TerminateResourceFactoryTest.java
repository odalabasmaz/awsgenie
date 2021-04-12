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
        TerminateResources<SQSResource> sqs = terminateResourceFactory.getTerminator(Service.SQS, credentialsProvider);
        assertThat(sqs, is(instanceOf(TerminateSqsResources.class)));
        TerminateResources<CloudwatchResource> cloudwatch = terminateResourceFactory.getTerminator(Service.CLOUDWATCH, credentialsProvider);
        assertThat(cloudwatch, is(instanceOf(TerminateCloudwatchResources.class)));
        TerminateResources<LambdaResource> lambda = terminateResourceFactory.getTerminator(Service.LAMBDA, credentialsProvider);
        assertThat(lambda, is(instanceOf(TerminateLambdaResources.class)));
        TerminateResources<DynamodbResource> dynamodb = terminateResourceFactory.getTerminator(Service.DYNAMODB, credentialsProvider);
        assertThat(dynamodb, is(instanceOf(TerminateDynamoDBResources.class)));
        TerminateResources<SNSResource> sns = terminateResourceFactory.getTerminator(Service.SNS, credentialsProvider);
        assertThat(sns, is(instanceOf(TerminateSnsResources.class)));
        TerminateResources<IAMRoleResource> iamRole = terminateResourceFactory.getTerminator(Service.IAM_ROLE, credentialsProvider);
        assertThat(iamRole, is(instanceOf(TerminateIamRoleResources.class)));
        TerminateResources<IAMPolicyResource> iamPolicy = terminateResourceFactory.getTerminator(Service.IAM_POLICY, credentialsProvider);
        assertThat(iamPolicy, is(instanceOf(TerminateIamPolicyResources.class)));
        TerminateResources<KinesisResource> kinesis = terminateResourceFactory.getTerminator(Service.KINESIS, credentialsProvider);
        assertThat(kinesis, is(instanceOf(TerminateKinesisResources.class)));
    }

    @Test(expected = OperationNotSupportedException.class)
    public void getTerminatorWithInvalidService() throws Exception {
        terminateResourceFactory.getTerminator(Service.CLOUDFRONT, credentialsProvider);
    }
}