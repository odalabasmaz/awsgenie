package io.github.odalabasmaz.awsgenie.terminator;

import io.github.odalabasmaz.awsgenie.fetcher.Service;
import io.github.odalabasmaz.awsgenie.fetcher.cloudwatch.CloudWatchResource;
import io.github.odalabasmaz.awsgenie.fetcher.credentials.AWSClientConfiguration;
import io.github.odalabasmaz.awsgenie.fetcher.dynamodb.DynamoDBResource;
import io.github.odalabasmaz.awsgenie.fetcher.iam.IAMPolicyResource;
import io.github.odalabasmaz.awsgenie.fetcher.iam.IAMRoleResource;
import io.github.odalabasmaz.awsgenie.fetcher.kinesis.KinesisResource;
import io.github.odalabasmaz.awsgenie.fetcher.lambda.LambdaResource;
import io.github.odalabasmaz.awsgenie.fetcher.sns.SNSResource;
import io.github.odalabasmaz.awsgenie.fetcher.sqs.SQSResource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import javax.naming.OperationNotSupportedException;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;


/**
 * @author Celal Emre CICEK
 * @version 9.04.2021
 */

@RunWith(MockitoJUnitRunner.class)
public class ResourceTerminatorFactoryTest {
    private ResourceTerminatorFactory resourceTerminatorFactory;

    @Before
    public void setUp() throws Exception {
        resourceTerminatorFactory = new ResourceTerminatorFactory();
    }

    @Test
    public void getTerminator() throws Exception {
        AWSClientConfiguration configuration = TerminatorHelper.getRegion1Account1Configuration();
        ResourceTerminator<SQSResource> sqs = resourceTerminatorFactory.getTerminator(Service.SQS, configuration);
        assertThat(sqs, is(instanceOf(SQSResourceTerminator.class)));
        ResourceTerminator<CloudWatchResource> cloudwatch = resourceTerminatorFactory.getTerminator(Service.CLOUDWATCH, configuration);
        assertThat(cloudwatch, is(instanceOf(CloudWatchResourceTerminator.class)));
        ResourceTerminator<LambdaResource> lambda = resourceTerminatorFactory.getTerminator(Service.LAMBDA, configuration);
        assertThat(lambda, is(instanceOf(LambdaResourceTerminator.class)));
        ResourceTerminator<DynamoDBResource> dynamodb = resourceTerminatorFactory.getTerminator(Service.DYNAMODB, configuration);
        assertThat(dynamodb, is(instanceOf(DynamoDBResourceTerminator.class)));
        ResourceTerminator<SNSResource> sns = resourceTerminatorFactory.getTerminator(Service.SNS, configuration);
        assertThat(sns, is(instanceOf(SNSResourceTerminator.class)));
        ResourceTerminator<IAMRoleResource> iamRole = resourceTerminatorFactory.getTerminator(Service.IAM_ROLE, configuration);
        assertThat(iamRole, is(instanceOf(IAMRoleResourceTerminator.class)));
        ResourceTerminator<IAMPolicyResource> iamPolicy = resourceTerminatorFactory.getTerminator(Service.IAM_POLICY, configuration);
        assertThat(iamPolicy, is(instanceOf(IAMPolicyResourceTerminator.class)));
        ResourceTerminator<KinesisResource> kinesis = resourceTerminatorFactory.getTerminator(Service.KINESIS, configuration);
        assertThat(kinesis, is(instanceOf(KinesisResourceTerminator.class)));
    }

    @Test(expected = OperationNotSupportedException.class)
    public void getTerminatorWithInvalidService() throws Exception {
        resourceTerminatorFactory.getTerminator(Service.CLOUDFRONT, TerminatorHelper.getRegion1Account1Configuration());
    }
}