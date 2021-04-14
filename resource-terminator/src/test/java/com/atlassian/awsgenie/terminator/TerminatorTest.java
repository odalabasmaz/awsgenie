package com.atlassian.awsgenie.terminator;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatchevents.AmazonCloudWatchEvents;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sqs.AmazonSQS;
import com.atlassian.awsgenie.fetcher.ResourceFetcher;
import com.atlassian.awsgenie.fetcher.ResourceFetcherConfiguration;
import com.atlassian.awsgenie.fetcher.ResourceFetcherFactory;
import com.atlassian.awsgenie.fetcher.Service;
import com.atlassian.awsgenie.fetcher.credentials.AWSClientProvider;
import com.atlassian.awsgenie.terminator.interceptor.InterceptorRegistry;
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
@PrepareForTest(AWSClientProvider.class)
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
    private ResourceFetcherFactory resourceFetcherFactory;
    @Mock
    private ResourceFetcher resourceFetcher;


    @Before
    public void setUp() throws Exception {
        InterceptorRegistry.getBeforeTerminateInterceptors().clear();
        InterceptorRegistry.getAfterTerminateInterceptors().clear();
        when(resourceFetcherFactory.getFetcher(any(Service.class), any(ResourceFetcherConfiguration.class)))
                .thenReturn(resourceFetcher);
        PowerMockito.mockStatic(AWSClientProvider.class);
        AWSClientProvider provider = PowerMockito.mock(AWSClientProvider.class);
        when(AWSClientProvider.getInstance(any())).thenReturn(provider);
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

    public ResourceFetcherFactory getFetchResourceFactory() {
        return resourceFetcherFactory;
    }

    public ResourceFetcher getFetchResources() {
        return resourceFetcher;
    }

    public AWSLambda getAmazonLambda() {
        return amazonLambda;
    }

    public AWSSecurityTokenService getAmazonSts() {
        return amazonSts;
    }
}