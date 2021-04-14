package com.atlassian.awsgenie.fetcher.iam;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.*;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClient;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityResult;
import com.atlassian.awsgenie.fetcher.ResourceFetcherConfiguration;
import com.atlassian.awsgenie.fetcher.credentials.AWSClientProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;


/**
 * @author Celal Emre CICEK
 * @version 12.04.2021
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        AWSClientProvider.class,
        AWSSecurityTokenServiceClientBuilder.class
})
@PowerMockIgnore({
        "javax.management.*", "javax.script.*"
})
public class IAMPolicyResourceFetcherTest {
    private static final String TEST_REGION = "us-west-2";

    @Mock
    private ResourceFetcherConfiguration resourceFetcherConfiguration;

    @Mock
    private AmazonIdentityManagement iamClient;

    @Mock
    private AWSClientProvider awsClientProvider;

    @Mock
    private AWSSecurityTokenServiceClientBuilder stsClientBuilder;

    @Mock
    private AWSSecurityTokenServiceClient stsClient;

    private IAMPolicyResourceFetcher IAMPolicyResourceFetcher;

    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(AWSClientProvider.class);
        when(AWSClientProvider.getInstance(resourceFetcherConfiguration))
                .thenReturn(awsClientProvider);
        when(awsClientProvider.getAmazonIAM())
                .thenReturn(iamClient);

        PowerMockito.mockStatic(AWSSecurityTokenServiceClientBuilder.class);
        when(AWSSecurityTokenServiceClientBuilder.standard()).thenReturn(stsClientBuilder);
        when(stsClientBuilder.build()).thenReturn(stsClient);
        when(stsClient.getCallerIdentity(org.mockito.Mockito.any(GetCallerIdentityRequest.class)))
                .thenReturn(new GetCallerIdentityResult().withAccount("acc1"));

        this.IAMPolicyResourceFetcher = new IAMPolicyResourceFetcher(resourceFetcherConfiguration);
    }

    @Test
    public void listResources() {
        when(iamClient.listPolicies(new ListPoliciesRequest()))
                .thenReturn(new ListPoliciesResult()
                        .withPolicies(
                                new Policy().withPolicyName("policy1"),
                                new Policy().withPolicyName("policy2")
                        )
                        .withMarker("marker"));
        when(iamClient.listPolicies(new ListPoliciesRequest().withMarker("marker")))
                .thenReturn(new ListPoliciesResult()
                        .withPolicies(
                                new Policy().withPolicyName("policy3"),
                                new Policy().withPolicyName("policy4")
                        ));

        List<String> actualPolicies = new ArrayList<>();
        IAMPolicyResourceFetcher.listResources(TEST_REGION, actualPolicies::addAll);

        verify(iamClient, times(2)).listPolicies(org.mockito.Mockito.any(ListPoliciesRequest.class));
        assertThat(actualPolicies.size(), is(equalTo(4)));
        assertThat(actualPolicies, hasItem("policy1"));
        assertThat(actualPolicies, hasItem("policy2"));
        assertThat(actualPolicies, hasItem("policy3"));
        assertThat(actualPolicies, hasItem("policy4"));
    }

    @Test
    public void fetchResources() {
        when(iamClient.getPolicy(new GetPolicyRequest().withPolicyArn("arn:aws:iam::acc1:policy/policy1")))
                .thenReturn(new GetPolicyResult().withPolicy(new Policy().withArn("arn1")));
        when(iamClient.getPolicy(new GetPolicyRequest().withPolicyArn("arn:aws:iam::acc1:policy/policy2")))
                .thenReturn(new GetPolicyResult().withPolicy(new Policy().withArn("arn2")));
        when(iamClient.getPolicy(new GetPolicyRequest().withPolicyArn("arn:aws:iam::acc1:policy/policy3")))
                .thenThrow(new NoSuchEntityException("policy3 does not exist"));
        List<String> resources = new ArrayList<>();
        resources.add("policy1");
        resources.add("policy2");
        resources.add("policy3");
        List<String> details = new ArrayList<>();
        List<IAMPolicyResource> actualResources = IAMPolicyResourceFetcher.fetchResources(TEST_REGION, resources, details);

        verify(iamClient, times(3)).getPolicy(org.mockito.Mockito.any(GetPolicyRequest.class));

        assertThat(actualResources.size(), is(equalTo(2)));
        assertThat(actualResources, hasItem(new IAMPolicyResource().setResourceName("arn1")));
        assertThat(actualResources, hasItem(new IAMPolicyResource().setResourceName("arn2")));

        assertThat(details.size(), is(equalTo(1)));
        assertThat(details, hasItem("!!! IAM Policy not exists: [policy3]"));
    }

    @Test
    public void getUsage() {
        Date lastAccessed = new Date(1618253487000L);
        when(iamClient.generateServiceLastAccessedDetails(new GenerateServiceLastAccessedDetailsRequest().withArn("arn1")))
                .thenReturn(new GenerateServiceLastAccessedDetailsResult().withJobId("job1"));
        when(iamClient.getServiceLastAccessedDetails(new GetServiceLastAccessedDetailsRequest().withJobId("job1")))
                .thenReturn(new GetServiceLastAccessedDetailsResult().withServicesLastAccessed(
                        new ServiceLastAccessed()
                                .withLastAuthenticated(lastAccessed)
                ));
        Date actual = (Date) IAMPolicyResourceFetcher.getUsage(TEST_REGION, "arn1", 7);
        assertThat(actual, is(equalTo(lastAccessed)));
    }
}