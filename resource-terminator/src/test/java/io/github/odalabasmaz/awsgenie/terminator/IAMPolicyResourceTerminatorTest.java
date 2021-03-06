package io.github.odalabasmaz.awsgenie.terminator;

import com.amazonaws.services.identitymanagement.model.DeletePolicyRequest;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityResult;
import io.github.odalabasmaz.awsgenie.fetcher.Resource;
import io.github.odalabasmaz.awsgenie.fetcher.Service;
import io.github.odalabasmaz.awsgenie.fetcher.iam.IAMPolicyResource;
import io.github.odalabasmaz.awsgenie.terminator.interceptor.AfterTerminateInterceptor;
import io.github.odalabasmaz.awsgenie.terminator.interceptor.BeforeTerminateInterceptor;
import io.github.odalabasmaz.awsgenie.terminator.interceptor.InterceptorRegistry;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;


/**
 * @author Celal Emre CICEK
 * @version 9.04.2021
 */

public class IAMPolicyResourceTerminatorTest extends TerminatorTest {
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

    private static final Set<Resource> TEST_FETCHED_RESOURCES = new LinkedHashSet<Resource>() {{
        add(new IAMPolicyResource()
                .setResourceName(POLICY_1));
        add(new IAMPolicyResource()
                .setResourceName(POLICY_2));
    }};

    @Mock
    private BeforeTerminateInterceptor beforeTerminateInterceptor;

    @Mock
    private AfterTerminateInterceptor afterTerminateInterceptor;

    private IAMPolicyResourceTerminator iamPolicyResourceTerminator;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        InterceptorRegistry.getBeforeTerminateInterceptors().clear();
        InterceptorRegistry.getAfterTerminateInterceptors().clear();
        this.iamPolicyResourceTerminator = new IAMPolicyResourceTerminator(TerminatorHelper.getRegion1Account1Configuration());
        this.iamPolicyResourceTerminator.setFetchResourceFactory(getFetchResourceFactory());
        doReturn(TEST_FETCHED_RESOURCES)
                .when(getFetchResources()).fetchResources(eq(TEST_REGION), eq(TEST_RESOURCES), org.mockito.Mockito.any(List.class));
        doReturn(DateTime.now().minus(TimeUnit.DAYS.toMillis(8)).toDate())
                .when(getFetchResources()).getUsage(eq(TEST_REGION), eq(POLICY_1), eq(7));
        doReturn(DateTime.now().minus(TimeUnit.DAYS.toMillis(5)).toDate())
                .when(getFetchResources()).getUsage(eq(TEST_REGION), eq(POLICY_2), eq(7));
        doReturn(new GetCallerIdentityResult().withAccount("account1"))
                .when(getAmazonSts()).getCallerIdentity(any(GetCallerIdentityRequest.class));
    }

    @Test
    public void terminateResourcesWithApply() throws Exception {
        iamPolicyResourceTerminator.terminateResource(TEST_REGION, service, TEST_RESOURCES, TEST_TICKET, true);
        ArgumentCaptor<DeletePolicyRequest> captor = ArgumentCaptor.forClass(DeletePolicyRequest.class);
        verify(getAmazonIdentityManagement()).deletePolicy(captor.capture());
        DeletePolicyRequest actualRequest = captor.getValue();
        assertThat(actualRequest.getPolicyArn(), is(equalTo("policy1")));
        verifyNoMoreInteractions(getAmazonIdentityManagement());
    }

    @Test
    public void terminateResourcesWithoutApply() throws Exception {
        iamPolicyResourceTerminator.terminateResource(TEST_REGION, service, TEST_RESOURCES, TEST_TICKET, false);
        verifyZeroInteractions(getAmazonIdentityManagement());
    }

    @Test
    public void interceptorsAreCalled() throws Exception {
        Set<Resource> deletableResources = new LinkedHashSet<Resource>() {{
            add(new IAMPolicyResource()
                    .setResourceName(POLICY_1));
        }};

        InterceptorRegistry.addInterceptor(beforeTerminateInterceptor);
        InterceptorRegistry.addInterceptor(afterTerminateInterceptor);
        iamPolicyResourceTerminator.terminateResource(TEST_REGION, service, TEST_RESOURCES, TEST_TICKET, false);
        verify(beforeTerminateInterceptor).intercept(eq(service), eq(deletableResources), eq(false));
        verify(afterTerminateInterceptor).intercept(eq(service), eq(deletableResources), eq(false));
    }
}