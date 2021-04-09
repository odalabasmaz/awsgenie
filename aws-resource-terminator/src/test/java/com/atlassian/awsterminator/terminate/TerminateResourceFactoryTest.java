package com.atlassian.awsterminator.terminate;

import com.amazonaws.auth.AWSCredentialsProvider;
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
        TerminateResources sqs = terminateResourceFactory.getTerminator("sqs", credentialsProvider);
        assertThat(sqs, is(instanceOf(TerminateSqsResources.class)));
        TerminateResources cloudwatch = terminateResourceFactory.getTerminator("cloudwatch", credentialsProvider);
        assertThat(cloudwatch, is(instanceOf(TerminateCloudwatchResources.class)));
        TerminateResources lambda = terminateResourceFactory.getTerminator("lambda", credentialsProvider);
        assertThat(lambda, is(instanceOf(TerminateLambdaResources.class)));
        TerminateResources dynamodb = terminateResourceFactory.getTerminator("dynamodb", credentialsProvider);
        assertThat(dynamodb, is(instanceOf(TerminateDynamoDBResources.class)));
        TerminateResources sns = terminateResourceFactory.getTerminator("sns", credentialsProvider);
        assertThat(sns, is(instanceOf(TerminateSnsResources.class)));
        TerminateResources iamRole = terminateResourceFactory.getTerminator("iamRole", credentialsProvider);
        assertThat(iamRole, is(instanceOf(TerminateIamRoleResources.class)));
        TerminateResources iamPolicy = terminateResourceFactory.getTerminator("iamPolicy", credentialsProvider);
        assertThat(iamPolicy, is(instanceOf(TerminateIamPolicyResources.class)));
        TerminateResources kinesis = terminateResourceFactory.getTerminator("kinesis", credentialsProvider);
        assertThat(kinesis, is(instanceOf(TerminateKinesisResources.class)));
    }

    @Test(expected = OperationNotSupportedException.class)
    public void getTerminatorWithInvalidService() throws Exception {
        terminateResourceFactory.getTerminator("invalid", credentialsProvider);
    }
}