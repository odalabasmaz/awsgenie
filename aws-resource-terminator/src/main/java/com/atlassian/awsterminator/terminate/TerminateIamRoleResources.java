package com.atlassian.awsterminator.terminate;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.identitymanagement.model.*;
import com.atlassian.awsterminator.interceptor.InterceptorRegistry;
import com.atlassian.awstool.terminate.FetchResourceFactory;
import com.atlassian.awstool.terminate.FetchResources;
import com.atlassian.awstool.terminate.Service;
import com.atlassian.awstool.terminate.iamrole.IAMRoleResource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Orhun Dalabasmaz
 * @version 06.04.2021
 * <p>
 * Details: Required permissions: iam:ListRoles, iam:DeleteRole
 * TODO: add more...
 */
public class TerminateIamRoleResources implements TerminateResources<IAMRoleResource> {
    private static final Logger LOGGER = LogManager.getLogger(TerminateIamRoleResources.class);

    private final AWSCredentialsProvider credentialsProvider;
    private AmazonIdentityManagement iamClient;
    private FetchResourceFactory<IAMRoleResource> fetchResourceFactory;

    public TerminateIamRoleResources(AWSCredentialsProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
    }

    @Override
    public void terminateResource(String region,
                                  Service service, List<String> resources, String ticket, boolean apply) throws Exception {
        AmazonIdentityManagement iamClient = getIAMClient(region);

        // Resources to be removed
        LinkedHashSet<String> rolesToDelete = new LinkedHashSet<>();
        LinkedHashSet<RoleEntity> inlinePoliciesToDelete = new LinkedHashSet<>();
        LinkedHashSet<RoleEntity> instanceProfilesToDetach = new LinkedHashSet<>();
        LinkedHashSet<RoleEntity> policiesToDetach = new LinkedHashSet<>();
        List<String> details = new LinkedList<>();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date endDate = new Date();
        Date referenceDate = new Date(endDate.getTime() - TimeUnit.DAYS.toMillis(7)); //TODO: make this configurable...

        FetchResources<IAMRoleResource> fetcher = getFetchResourceFactory().getFetcher(Service.IAM_ROLE, credentialsProvider);
        List<IAMRoleResource> iamRoleResourceList = fetcher.fetchResources(region, resources, details);

        for (IAMRoleResource iamRoleResource : iamRoleResourceList) {
            String roleName = iamRoleResource.getResourceName();
            if (iamRoleResource.getLastUsedDate() != null && iamRoleResource.getLastUsedDate().after(referenceDate)) {
                details.add("IAM role seems in use, not deleting: [" + roleName + "], lastUsageDate: [" + sdf.format(referenceDate) + "]");
                LOGGER.warn("IAM role seems in use, not deleting: [" + roleName + "], lastUsageDate: [" + sdf.format(referenceDate) + "]");
                continue;
            }
            details.add("IAM Role will be deleted: " + roleName);
            rolesToDelete.add(roleName);

            List<RoleEntity> inlinePolicyNames = iamClient.listRolePolicies(new ListRolePoliciesRequest().withRoleName(roleName))
                    .getPolicyNames().stream().map(p -> new RoleEntity(roleName, p)).collect(Collectors.toList());
            details.add("-- Inline policies will be deleted: " + inlinePolicyNames);
            inlinePoliciesToDelete.addAll(inlinePolicyNames);

            List<RoleEntity> instanceProfiles = iamClient.listInstanceProfilesForRole(new ListInstanceProfilesForRoleRequest().withRoleName(roleName))
                    .getInstanceProfiles().stream().map(p -> new RoleEntity(roleName, p.getInstanceProfileName())).collect(Collectors.toList());
            details.add("-- Instance profiles will be detached: " + instanceProfiles);
            instanceProfilesToDetach.addAll(instanceProfiles);

            List<RoleEntity> attachedPolicies = iamClient.listAttachedRolePolicies(new ListAttachedRolePoliciesRequest().withRoleName(roleName))
                    .getAttachedPolicies().stream().map(p -> new RoleEntity(roleName, p.getPolicyArn())).collect(Collectors.toList());
            details.add("-- Attached profiles will be detached: " + attachedPolicies);
            policiesToDetach.addAll(attachedPolicies);
        }

        StringBuilder info = new StringBuilder()
                .append("The resources will be terminated regarding ").append(ticket).append("\n")
                .append("* Dry-Run: ").append(!apply).append("\n")
                .append("* IAM roles: ").append(rolesToDelete).append("\n");

        info.append("Details:\n");
        details.forEach(d -> info.append("-- ").append(d).append("\n"));
        LOGGER.info(info);

        InterceptorRegistry.getBeforeTerminateInterceptors()
                .forEach(interceptor -> interceptor.intercept(service, iamRoleResourceList, info.toString(), apply));

        if (apply) {
            LOGGER.info("Terminating the resources...");
            inlinePoliciesToDelete.forEach(p -> iamClient.deleteRolePolicy(new DeleteRolePolicyRequest().withRoleName(p.roleName).withPolicyName(p.entityName)));
            instanceProfilesToDetach.forEach(p -> iamClient.removeRoleFromInstanceProfile(new RemoveRoleFromInstanceProfileRequest().withRoleName(p.roleName).withInstanceProfileName(p.entityName)));
            policiesToDetach.forEach(p -> iamClient.detachRolePolicy(new DetachRolePolicyRequest().withRoleName(p.roleName).withPolicyArn(p.entityName)));
            rolesToDelete.forEach(r -> iamClient.deleteRole(new DeleteRoleRequest().withRoleName(r)));
        }

        InterceptorRegistry.getAfterTerminateInterceptors()
                .forEach(interceptor -> interceptor.intercept(service, iamRoleResourceList, info.toString(), apply));

        LOGGER.info("Succeed.");
    }

    void setIAMClient(AmazonIdentityManagement iamClient) {
        this.iamClient = iamClient;
    }

    private AmazonIdentityManagement getIAMClient(String region) {
        if (this.iamClient != null) {
            return this.iamClient;
        } else {
            return AmazonIdentityManagementClient
                    .builder()
                    .withRegion(region)
                    .withCredentials(credentialsProvider)
                    .build();
        }
    }

    void setFetchResourceFactory(FetchResourceFactory<IAMRoleResource> fetchResourceFactory) {
        this.fetchResourceFactory = fetchResourceFactory;
    }

    private FetchResourceFactory<IAMRoleResource> getFetchResourceFactory() {
        if (this.fetchResourceFactory != null) {
            return this.fetchResourceFactory;
        } else {
            return new FetchResourceFactory<>();
        }
    }

    /**
     * Generic key-value for Role and the entity: policy, instance profile, etc.
     */
    private static class RoleEntity {
        String roleName;
        String entityName;

        RoleEntity(String roleName, String entityName) {
            this.roleName = roleName;
            this.entityName = entityName;
        }

        public String toString() {
            return roleName + "/" + entityName;
        }
    }
}
