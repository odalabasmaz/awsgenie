package com.atlassian.awsterminator.terminate;

import com.amazonaws.services.identitymanagement.model.DeletePolicyRequest;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityResult;
import com.atlassian.awsterminator.interceptor.AfterTerminateInterceptor;
import com.atlassian.awsterminator.interceptor.BeforeTerminateInterceptor;
import com.atlassian.awsterminator.interceptor.InterceptorRegistry;
import com.atlassian.awstool.terminate.AWSResource;
import com.atlassian.awstool.terminate.Service;
import com.atlassian.awstool.terminate.iam.IAMPolicyResource;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

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

public class TerminateIamPolicyResourcesTest extends TerminatorTest {
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
    private BeforeTerminateInterceptor beforeTerminateInterceptor;

    @Mock
    private AfterTerminateInterceptor afterTerminateInterceptor;

    private TerminateIamPolicyResources terminateIamPolicyResources;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        InterceptorRegistry.getBeforeTerminateInterceptors().clear();
        InterceptorRegistry.getAfterTerminateInterceptors().clear();
        this.terminateIamPolicyResources = new TerminateIamPolicyResources(TerminatorHelper.getRegion1Account1Configuration());
        this.terminateIamPolicyResources.setFetchResourceFactory(getFetchResourceFactory());
        doReturn(TEST_FETCHED_RESOURCES)
                .when(getFetchResources()).fetchResources(eq(TEST_REGION), eq(TEST_RESOURCES), org.mockito.Mockito.any(List.class));
        doReturn(DateTime.now().minus(TimeUnit.DAYS.toMillis(8)).toDate())
                .when(getFetchResources()).getUsage(eq(TEST_REGION), eq(POLICY_1));
        doReturn(DateTime.now().minus(TimeUnit.DAYS.toMillis(5)).toDate())
                .when(getFetchResources()).getUsage(eq(TEST_REGION), eq(POLICY_2));
        doReturn(new GetCallerIdentityResult().withAccount("account1"))
                .when(getAmazonSts()).getCallerIdentity(any(GetCallerIdentityRequest.class));


    }

    @Test
    public void terminateResourcesWithApply() throws Exception {
        terminateIamPolicyResources.terminateResource(TEST_REGION, service, TEST_RESOURCES, TEST_TICKET, true);
        ArgumentCaptor<DeletePolicyRequest> captor = ArgumentCaptor.forClass(DeletePolicyRequest.class);
        verify(getAmazonIdentityManagement()).deletePolicy(captor.capture());
        DeletePolicyRequest actualRequest = captor.getValue();
        assertThat(actualRequest.getPolicyArn(), is(equalTo("policy1")));
        verifyNoMoreInteractions(getAmazonIdentityManagement());
    }

    @Test
    public void terminateResourcesWithoutApply() throws Exception {
        terminateIamPolicyResources.terminateResource(TEST_REGION, service, TEST_RESOURCES, TEST_TICKET, false);
        verifyZeroInteractions(getAmazonIdentityManagement());
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