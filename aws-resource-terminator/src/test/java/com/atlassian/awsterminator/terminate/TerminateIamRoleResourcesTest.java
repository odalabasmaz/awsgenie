package com.atlassian.awsterminator.terminate;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.DeleteRoleRequest;
import com.atlassian.awsterminator.interceptor.AfterTerminateInterceptor;
import com.atlassian.awsterminator.interceptor.BeforeTerminateInterceptor;
import com.atlassian.awsterminator.interceptor.InterceptorRegistry;
import com.atlassian.awstool.terminate.AWSResource;
import com.atlassian.awstool.terminate.FetchResourceFactory;
import com.atlassian.awstool.terminate.FetchResources;
import com.atlassian.awstool.terminate.iamrole.IAMRoleResource;
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
public class TerminateIamRoleResourcesTest {
    private static final String TEST_REGION = "us-west-2";
    private static final String TEST_TICKET = "TEST-TICKET";

    private static final List<String> TEST_RESOURCES = new ArrayList<String>() {{
        add("role1");
        add("role2");
        add("role3");
    }};

    private static final List<AWSResource> TEST_FETCHED_RESOURCES = new ArrayList<AWSResource>() {{
        add(new IAMRoleResource()
                .setResourceName("role1")
                .setLastUsedDate(DateTime.now().minus(TimeUnit.DAYS.toMillis(8)).toDate()));
        add(new IAMRoleResource()
                .setResourceName("role2")
                .setLastUsedDate(DateTime.now().minus(TimeUnit.DAYS.toMillis(5)).toDate()));
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

    private TerminateIamRoleResources terminateIamRoleResources;

    @Before
    public void setUp() throws Exception {
        InterceptorRegistry.getBeforeTerminateInterceptors().clear();
        InterceptorRegistry.getAfterTerminateInterceptors().clear();
        this.terminateIamRoleResources = new TerminateIamRoleResources(credentialsProvider);
        terminateIamRoleResources.setIAMClient(iamClient);
        terminateIamRoleResources.setFetchResourceFactory(fetchResourceFactory);

        when(fetchResourceFactory.getFetcher("iamRole", credentialsProvider))
                .thenReturn(fetchResources);
        doReturn(TEST_FETCHED_RESOURCES)
                .when(fetchResources).fetchResources(eq(TEST_REGION), eq(TEST_RESOURCES), org.mockito.Mockito.any(List.class));
    }

    @Test
    public void terminateResourcesWithApply() throws Exception {
        terminateIamRoleResources.terminateResource(TEST_REGION, "iamRole", TEST_RESOURCES, TEST_TICKET, true);

        ArgumentCaptor<DeleteRoleRequest> captor = ArgumentCaptor.forClass(DeleteRoleRequest.class);
        verify(iamClient).deleteRole(captor.capture());
        DeleteRoleRequest actualRequest = captor.getValue();
        assertThat(actualRequest.getRoleName(), is(equalTo("role1")));
        verifyNoMoreInteractions(iamClient);
    }

    @Test
    public void terminateResourcesWithoutApply() throws Exception {
        terminateIamRoleResources.terminateResource(TEST_REGION, "iamRole", TEST_RESOURCES, TEST_TICKET, false);
        verifyZeroInteractions(iamClient);
    }

    @Test
    public void interceptorsAreCalled() throws Exception {
        InterceptorRegistry.addInterceptor(beforeTerminateInterceptor);
        InterceptorRegistry.addInterceptor(afterTerminateInterceptor);
        terminateIamRoleResources.terminateResource(TEST_REGION, "iamRole", TEST_RESOURCES, TEST_TICKET, false);
        verify(beforeTerminateInterceptor).intercept(eq("iamRole"), eq(TEST_FETCHED_RESOURCES), any(String.class), eq(false));
        verify(afterTerminateInterceptor).intercept(eq("iamRole"), eq(TEST_FETCHED_RESOURCES), any(String.class), eq(false));
    }
}