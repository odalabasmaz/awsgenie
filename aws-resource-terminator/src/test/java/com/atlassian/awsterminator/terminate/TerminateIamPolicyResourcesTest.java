package com.atlassian.awsterminator.terminate;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.DeletePolicyRequest;
import com.atlassian.awsterminator.interceptor.AfterTerminateInterceptor;
import com.atlassian.awsterminator.interceptor.BeforeTerminateInterceptor;
import com.atlassian.awsterminator.interceptor.InterceptorRegistry;
import com.atlassian.awstool.terminate.AWSResource;
import com.atlassian.awstool.terminate.FetchResourceFactory;
import com.atlassian.awstool.terminate.FetchResources;
import com.atlassian.awstool.terminate.Service;
import com.atlassian.awstool.terminate.iamPolicy.IAMPolicyResource;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;


/**
 * @author Celal Emre CICEK
 * @version 9.04.2021
 */

@RunWith(MockitoJUnitRunner.class)
public class TerminateIamPolicyResourcesTest {
    private static final String TEST_REGION = "us-west-2";
    private static final String TEST_TICKET = "TEST-TICKET";
    public static final String POLICY_1 = "policy1";
    public static final String POLICY_2 = "policy2";
    private final Service service = Service.IAM_POLICY;

    private static final List<String> TEST_RESOURCES = new ArrayList<String>() {{
        add(POLICY_1);
        add(POLICY_2);
        add("policy3");
    }};

    private static final List<AWSResource> TEST_FETCHED_RESOURCES = new ArrayList<AWSResource>() {{
        add(new IAMPolicyResource()
                .setResourceName(POLICY_1));
        add(new IAMPolicyResource()
                .setResourceName(POLICY_2));
    }};

    @Mock
    private AmazonIdentityManagement iamClient;

    @Mock
    private AWSCredentialsProvider credentialsProvider;

    @Mock
    private FetchResourceFactory fetchResourceFactory;

    @Mock
    private FetchResources fetchResources;

    @Mock
    private BeforeTerminateInterceptor beforeTerminateInterceptor;

    @Mock
    private AfterTerminateInterceptor afterTerminateInterceptor;

    private TerminateIamPolicyResources terminateIamPolicyResources;

    @Before
    public void setUp() throws Exception {
        InterceptorRegistry.getBeforeTerminateInterceptors().clear();
        InterceptorRegistry.getAfterTerminateInterceptors().clear();
        this.terminateIamPolicyResources = new TerminateIamPolicyResources(credentialsProvider);
        terminateIamPolicyResources.setIAMClient(iamClient);
        terminateIamPolicyResources.setFetchResourceFactory(fetchResourceFactory);

        when(fetchResourceFactory.getFetcher(service, credentialsProvider))
                .thenReturn(fetchResources);
        doReturn(TEST_FETCHED_RESOURCES)
                .when(fetchResources).fetchResources(eq(TEST_REGION), eq(TEST_RESOURCES), org.mockito.Mockito.any(List.class));
        doReturn(DateTime.now().minus(TimeUnit.DAYS.toMillis(8)).toDate())
                .when(fetchResources).getUsage(eq(TEST_REGION), eq(POLICY_1));
        doReturn(DateTime.now().minus(TimeUnit.DAYS.toMillis(5)).toDate())
                .when(fetchResources).getUsage(eq(TEST_REGION), eq(POLICY_2));
    }

    @Test
    public void terminateResourcesWithApply() throws Exception {
        terminateIamPolicyResources.terminateResource(TEST_REGION, service, TEST_RESOURCES, TEST_TICKET, true);

        ArgumentCaptor<DeletePolicyRequest> captor = ArgumentCaptor.forClass(DeletePolicyRequest.class);
        verify(iamClient).deletePolicy(captor.capture());
        DeletePolicyRequest actualRequest = captor.getValue();
        assertThat(actualRequest.getPolicyArn(), is(equalTo("policy1")));
        verifyNoMoreInteractions(iamClient);
    }

    @Test
    public void terminateResourcesWithoutApply() throws Exception {
        terminateIamPolicyResources.terminateResource(TEST_REGION, service, TEST_RESOURCES, TEST_TICKET, false);
        verifyZeroInteractions(iamClient);
    }

    @Test
    public void interceptorsAreCalled() throws Exception {
        InterceptorRegistry.addInterceptor(beforeTerminateInterceptor);
        InterceptorRegistry.addInterceptor(afterTerminateInterceptor);
        terminateIamPolicyResources.terminateResource(TEST_REGION, service, TEST_RESOURCES, TEST_TICKET, false);
        verify(beforeTerminateInterceptor).intercept(eq(service), eq(TEST_FETCHED_RESOURCES), any(String.class), eq(false));
        verify(afterTerminateInterceptor).intercept(eq(service), eq(TEST_FETCHED_RESOURCES), any(String.class), eq(false));
    }
}