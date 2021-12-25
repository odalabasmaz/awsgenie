package io.github.odalabasmaz.awsgenie.fetcher.iam;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.*;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClient;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityResult;
import io.github.odalabasmaz.awsgenie.fetcher.ResourceFetcherConfiguration;
import io.github.odalabasmaz.awsgenie.fetcher.credentials.AWSClientProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.*;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

/**
 * @author Celal Emre CICEK
 * @version 13.04.2021
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        AWSClientProvider.class,
        AWSSecurityTokenServiceClientBuilder.class
})
@PowerMockIgnore({
        "javax.management.*", "javax.script.*"
})
public class IAMRoleResourceFetcherTest {
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

    private IAMRoleResourceFetcher IAMRoleResourceFetcher;

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

        this.IAMRoleResourceFetcher = new IAMRoleResourceFetcher(resourceFetcherConfiguration);
    }

    @Test
    public void listResources() {
        when(iamClient.listRoles(new ListRolesRequest()))
                .thenReturn(new ListRolesResult()
                        .withRoles(
                                new Role().withRoleName("role1"),
                                new Role().withRoleName("role2")
                        )
                        .withMarker("marker"));
        when(iamClient.listRoles(new ListRolesRequest().withMarker("marker")))
                .thenReturn(new ListRolesResult()
                        .withRoles(
                                new Role().withRoleName("role3"),
                                new Role().withRoleName("role4")
                        ));

        List<String> actualRoles = new ArrayList<>();
        IAMRoleResourceFetcher.listResources(TEST_REGION, actualRoles::addAll);

        verify(iamClient, times(2)).listRoles(org.mockito.Mockito.any(ListRolesRequest.class));
        assertThat(actualRoles.size(), is(equalTo(4)));
        assertThat(actualRoles, hasItem("role1"));
        assertThat(actualRoles, hasItem("role2"));
        assertThat(actualRoles, hasItem("role3"));
        assertThat(actualRoles, hasItem("role4"));
    }

    @Test
    public void fetchResources() {
        when(iamClient.getRole(new GetRoleRequest().withRoleName("role1")))
                .thenReturn(new GetRoleResult().withRole(new Role()
                        .withRoleName("role1")));
        when(iamClient.getRole(new GetRoleRequest().withRoleName("role2")))
                .thenReturn(new GetRoleResult().withRole(new Role()
                        .withRoleName("role2")));
        when(iamClient.getRole(new GetRoleRequest().withRoleName("role3")))
                .thenThrow(new NoSuchEntityException("role3 does not exists"));

        when(iamClient.listRolePolicies(new ListRolePoliciesRequest().withRoleName("role1")))
                .thenReturn(new ListRolePoliciesResult().withPolicyNames("policy1", "policy2"));
        when(iamClient.listInstanceProfilesForRole(new ListInstanceProfilesForRoleRequest().withRoleName("role1")))
                .thenReturn(new ListInstanceProfilesForRoleResult());
        when(iamClient.listAttachedRolePolicies(new ListAttachedRolePoliciesRequest().withRoleName("role1")))
                .thenReturn(new ListAttachedRolePoliciesResult().withAttachedPolicies(
                        new AttachedPolicy().withPolicyArn("policyArn1"),
                        new AttachedPolicy().withPolicyArn("policyArn2")
                ));

        when(iamClient.listRolePolicies(new ListRolePoliciesRequest().withRoleName("role2")))
                .thenReturn(new ListRolePoliciesResult());
        when(iamClient.listInstanceProfilesForRole(new ListInstanceProfilesForRoleRequest().withRoleName("role2")))
                .thenReturn(new ListInstanceProfilesForRoleResult().withInstanceProfiles(
                        new InstanceProfile().withInstanceProfileName("profile1"),
                        new InstanceProfile().withInstanceProfileName("profile2")
                ));
        when(iamClient.listAttachedRolePolicies(new ListAttachedRolePoliciesRequest().withRoleName("role2")))
                .thenReturn(new ListAttachedRolePoliciesResult());

        List<String> resources = new ArrayList<>();
        resources.add("role1");
        resources.add("role2");
        resources.add("role3");
        List<String> details = new ArrayList<>();
        Set<IAMRoleResource> actualResources = IAMRoleResourceFetcher.fetchResources(TEST_REGION, resources, details);

        verify(iamClient, times(3)).getRole(org.mockito.Mockito.any(GetRoleRequest.class));
        verify(iamClient, times(2)).listRolePolicies(org.mockito.Mockito.any(ListRolePoliciesRequest.class));
        verify(iamClient, times(2)).listInstanceProfilesForRole(org.mockito.Mockito.any(ListInstanceProfilesForRoleRequest.class));
        verify(iamClient, times(2)).listAttachedRolePolicies(org.mockito.Mockito.any(ListAttachedRolePoliciesRequest.class));

        assertThat(actualResources.size(), is(equalTo(2)));
        assertThat(actualResources, hasItem(new IAMRoleResource()
                .setResourceName("role1")
                .setInlinePolicies(new HashSet<IAMEntity>() {{
                    add(new IAMEntity("role1", "policy1"));
                    add(new IAMEntity("role1", "policy2"));
                }})
                .setAttachedPolicies(new HashSet<IAMEntity>() {{
                    add(new IAMEntity("role1", "policyArn1"));
                    add(new IAMEntity("role1", "policyArn2"));
                }})
        ));
        assertThat(actualResources, hasItem(new IAMRoleResource()
                .setResourceName("role2")
                .setInstanceProfiles(new HashSet<IAMEntity>() {{
                    add(new IAMEntity("role2", "profile1"));
                    add(new IAMEntity("role2", "profile2"));
                }})
        ));

        assertThat(details.size(), is(equalTo(1)));
        assertThat(details, hasItem("!!! IAM Role not exists: [role3]"));
    }

    @Test
    public void getUsage() {
        Date lastAccessed = new Date(1618253487000L);

        when(iamClient.getRole(new GetRoleRequest().withRoleName("role1")))
                .thenReturn(new GetRoleResult().withRole(new Role().withRoleLastUsed(new RoleLastUsed()
                        .withLastUsedDate(lastAccessed))));

        Date actual = (Date) IAMRoleResourceFetcher.getUsage(TEST_REGION, "role1", 7);
        assertThat(actual, is(equalTo(lastAccessed)));
    }
}