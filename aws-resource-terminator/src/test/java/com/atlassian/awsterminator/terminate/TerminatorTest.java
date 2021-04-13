package com.atlassian.awsterminator.terminate;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatchevents.AmazonCloudWatchEvents;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sqs.AmazonSQS;
import com.atlassian.awsterminator.interceptor.InterceptorRegistry;
import com.atlassian.awstool.terminate.FetchResourceFactory;
import com.atlassian.awstool.terminate.FetchResources;
import com.atlassian.awstool.terminate.FetcherConfiguration;
import com.atlassian.awstool.terminate.Service;
import credentials.AwsClientProvider;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;


/**
 * @author Celal Emre CICEK
 * @version 9.04.2021
 */

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.net.ssl.*", "javax.management.*", "javax.crypto.*", "javax.security.*", "javax.script.*", "sun.security.*"})
@PrepareForTest(AwsClientProvider.class)
@Ignore
public abstract class TerminatorTest {

    @Mock
    private AmazonCloudWatch cloudWatchClient;
    @Mock
    private AmazonSQS amazonSQS;
    @Mock
    private AmazonKinesis amazonKinesis;
    @Mock
    private AmazonSNS amazonSNS;
    @Mock
    private AmazonCloudWatchEvents cloudWatchEvents;
    @Mock
    private AmazonIdentityManagement amazonIdentityManagement;
    @Mock
    private AmazonDynamoDB amazonDynamoDB;
    @Mock
    private AWSSecurityTokenService amazonSts;
    @Mock
    private AWSLambda amazonLambda;
    @Mock
    private FetchResourceFactory fetchResourceFactory;
    @Mock
    private FetchResources fetchResources;


    @Before
    public void setUp() throws Exception {
        InterceptorRegistry.getBeforeTerminateInterceptors().clear();
        InterceptorRegistry.getAfterTerminateInterceptors().clear();
        when(fetchResourceFactory.getFetcher(any(Service.class), any(FetcherConfiguration.class)))
                .thenReturn(fetchResources);
        PowerMockito.mockStatic(AwsClientProvider.class);
        AwsClientProvider provider = PowerMockito.mock(AwsClientProvider.class);
        when(AwsClientProvider.getInstance(any())).thenReturn(provider);
        when(provider.getAmazonCloudWatch()).thenReturn(cloudWatchClient);
        when(provider.getAmazonCloudWatchEvents()).thenReturn(cloudWatchEvents);
        when(provider.getAmazonDynamoDB()).thenReturn(amazonDynamoDB);
        when(provider.getAmazonIAM()).thenReturn(amazonIdentityManagement);
        when(provider.getAmazonKinesis()).thenReturn(amazonKinesis);
        when(provider.getAmazonSQS()).thenReturn(amazonSQS);
        when(provider.getAmazonSNS()).thenReturn(amazonSNS);
        when(provider.getAmazonLambda()).thenReturn(amazonLambda);
        when(provider.getAmazonSts()).thenReturn(amazonSts);
    }

    public AmazonCloudWatch getCloudWatchClient() {
        return cloudWatchClient;
    }

    public AmazonSQS getAmazonSQS() {
        return amazonSQS;
    }

    public AmazonKinesis getAmazonKinesis() {
        return amazonKinesis;
    }

    public AmazonSNS getAmazonSNS() {
        return amazonSNS;
    }

    public AmazonCloudWatchEvents getCloudWatchEvents() {
        return cloudWatchEvents;
    }

    public AmazonIdentityManagement getAmazonIdentityManagement() {
        return amazonIdentityManagement;
    }

    public AmazonDynamoDB getAmazonDynamoDB() {
        return amazonDynamoDB;
    }

    public FetchResourceFactory getFetchResourceFactory() {
        return fetchResourceFactory;
    }

    public FetchResources getFetchResources() {
        return fetchResources;
    }

    public AWSLambda getAmazonLambda() {
        return amazonLambda;
    }

    public AWSSecurityTokenService getAmazonSts() {
        return amazonSts;
    }
}