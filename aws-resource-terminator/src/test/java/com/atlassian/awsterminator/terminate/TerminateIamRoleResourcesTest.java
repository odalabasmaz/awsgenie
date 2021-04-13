package com.atlassian.awsterminator.terminate;

import com.amazonaws.services.identitymanagement.model.*;
import com.atlassian.awsterminator.interceptor.AfterTerminateInterceptor;
import com.atlassian.awsterminator.interceptor.BeforeTerminateInterceptor;
import com.atlassian.awsterminator.interceptor.InterceptorRegistry;
import com.atlassian.awstool.terminate.AWSResource;
import com.atlassian.awstool.terminate.Service;
import com.atlassian.awstool.terminate.iam.IAMRoleResource;
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

public class TerminateIamRoleResourcesTest extends TerminatorTest {
    private static final String TEST_REGION = "us-west-2";
    private static final String TEST_TICKET = "TEST-TICKET";
    public static final String ROLE_1 = "role1";
    public static final String ROLE_2 = "role2";
    private final Service service = Service.IAM_ROLE;

    private static final List<String> TEST_RESOURCES = new ArrayList<String>() {{
        add(ROLE_1);
        add(ROLE_2);
        add("role3");
    }};

    private static final List<AWSResource> TEST_FETCHED_RESOURCES = new ArrayList<AWSResource>() {{
        add(new IAMRoleResource()
                .setResourceName(ROLE_1));
        add(new IAMRoleResource()
                .setResourceName(ROLE_2));
    }};
    @Mock
    private BeforeTerminateInterceptor beforeTerminateInterceptor;

    @Mock
    private AfterTerminateInterceptor afterTerminateInterceptor;

    private TerminateIamRoleResources terminateIamRoleResources;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        InterceptorRegistry.getBeforeTerminateInterceptors().clear();
        InterceptorRegistry.getAfterTerminateInterceptors().clear();
        this.terminateIamRoleResources = new TerminateIamRoleResources(TerminatorHelper.getRegion1Account1Configuration());
        terminateIamRoleResources.setFetchResourceFactory(getFetchResourceFactory());

        doReturn(TEST_FETCHED_RESOURCES)
                .when(getFetchResources()).fetchResources(eq(TEST_REGION), eq(TEST_RESOURCES), org.mockito.Mockito.any(List.class));
        doReturn(DateTime.now().minus(TimeUnit.DAYS.toMillis(8)).toDate())
                .when(getFetchResources()).getUsage(eq(TEST_REGION), eq(ROLE_1), eq(7));
        doReturn(DateTime.now().minus(TimeUnit.DAYS.toMillis(5)).toDate())
                .when(getFetchResources()).getUsage(eq(TEST_REGION), eq(ROLE_2), eq(7));
    }

    @Test
    public void terminateResourcesWithApply() throws Exception {
        doReturn(new ListRolePoliciesResult()).when(getAmazonIdentityManagement()).listRolePolicies(new ListRolePoliciesRequest().withRoleName(ROLE_1));
        doReturn(new ListInstanceProfilesForRoleResult()).when(getAmazonIdentityManagement()).listInstanceProfilesForRole(new ListInstanceProfilesForRoleRequest().withRoleName(ROLE_1));
        doReturn(new ListAttachedRolePoliciesResult()).when(getAmazonIdentityManagement()).listAttachedRolePolicies(new ListAttachedRolePoliciesRequest().withRoleName(ROLE_1));


        terminateIamRoleResources.terminateResource(TEST_REGION, service, TEST_RESOURCES, TEST_TICKET, true);
        ArgumentCaptor<DeleteRoleRequest> captor = ArgumentCaptor.forClass(DeleteRoleRequest.class);
        verify(getAmazonIdentityManagement()).deleteRole(captor.capture());
        DeleteRoleRequest actualRequest = captor.getValue();
        assertThat(actualRequest.getRoleName(), is(equalTo("role1")));
    }

    @Test
    public void terminateResourcesWithoutApply() throws Exception {
        terminateIamRoleResources.terminateResource(TEST_REGION, service, TEST_RESOURCES, TEST_TICKET, false);
        verifyZeroInteractions(getAmazonIdentityManagement());
    }

    @Test
    public void interceptorsAreCalled() throws Exception {
        InterceptorRegistry.addInterceptor(beforeTerminateInterceptor);
        InterceptorRegistry.addInterceptor(afterTerminateInterceptor);
        terminateIamRoleResources.terminateResource(TEST_REGION, service, TEST_RESOURCES, TEST_TICKET, false);
        verify(beforeTerminateInterceptor).intercept(eq(service), eq(TEST_FETCHED_RESOURCES), any(String.class), eq(false));
        verify(afterTerminateInterceptor).intercept(eq(service), eq(TEST_FETCHED_RESOURCES), any(String.class), eq(false));
    }
}