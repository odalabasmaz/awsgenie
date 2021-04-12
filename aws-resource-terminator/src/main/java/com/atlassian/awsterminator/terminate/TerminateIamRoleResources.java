package com.atlassian.awsterminator.terminate;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.DeleteRolePolicyRequest;
import com.amazonaws.services.identitymanagement.model.DeleteRoleRequest;
import com.amazonaws.services.identitymanagement.model.DetachRolePolicyRequest;
import com.amazonaws.services.identitymanagement.model.RemoveRoleFromInstanceProfileRequest;
import com.atlassian.awsterminator.interceptor.InterceptorRegistry;
import com.atlassian.awstool.terminate.FetchResourceFactory;
import com.atlassian.awstool.terminate.FetchResources;
import com.atlassian.awstool.terminate.FetcherConfiguration;
import com.atlassian.awstool.terminate.Service;
import com.atlassian.awstool.terminate.iamrole.IAMRoleResource;
import com.atlassian.awstool.terminate.iamrole.IamEntity;
import credentials.AwsClientConfiguration;
import credentials.AwsClientProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Orhun Dalabasmaz
 * @version 06.04.2021
 * <p>
 * Details: Required permissions: iam:ListRoles, iam:DeleteRole
 * TODO: add more...
 */
public class TerminateIamRoleResources extends TerminateResourcesWithProvider implements TerminateResources<IAMRoleResource> {
    private static final Logger LOGGER = LogManager.getLogger(TerminateIamRoleResources.class);

    private FetchResourceFactory<IAMRoleResource> fetchResourceFactory;

    public TerminateIamRoleResources(AwsClientConfiguration configuration) {
        super(configuration);
    }

    @Override
    public void terminateResource(String region,
                                  Service service, List<String> resources, String ticket, boolean apply) throws Exception {
        AmazonIdentityManagement iamClient = AwsClientProvider.getInstance(getConfiguration()).getAmazonIAM();

        // Resources to be removed
        LinkedHashSet<String> rolesToDelete = new LinkedHashSet<>();
        LinkedHashSet<IamEntity> inlinePoliciesToDelete = new LinkedHashSet<>();
        LinkedHashSet<IamEntity> instanceProfilesToDetach = new LinkedHashSet<>();
        LinkedHashSet<IamEntity> policiesToDetach = new LinkedHashSet<>();
        List<String> details = new LinkedList<>();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date endDate = new Date();
        Date referenceDate = new Date(endDate.getTime() - TimeUnit.DAYS.toMillis(7)); //TODO: make this configurable...

        FetchResources<IAMRoleResource> fetcher = getFetchResourceFactory().getFetcher(Service.IAM_ROLE, new FetcherConfiguration(getConfiguration()));
        List<IAMRoleResource> iamRoleResourceList = fetcher.fetchResources(region, resources, details);

        for (IAMRoleResource iamRoleResource : iamRoleResourceList) {
            String roleName = iamRoleResource.getResourceName();
            Date lastUsedDate = (Date) fetcher.getUsage(region, roleName);
            if (lastUsedDate != null && lastUsedDate.after(referenceDate)) {
                details.add("IAM role seems in use, not deleting: [" + roleName + "], lastUsageDate: [" + sdf.format(lastUsedDate) + "]");
                LOGGER.warn("IAM role seems in use, not deleting: [" + roleName + "], lastUsageDate: [" + sdf.format(lastUsedDate) + "]");
                continue;
            }
            details.add("IAM Role will be deleted: " + roleName);
            rolesToDelete.add(roleName);

            LinkedHashSet<IamEntity> inlinePolicyNames = iamRoleResource.getInlinePolicies();
            details.add("-- Inline policies will be deleted: " + inlinePolicyNames);
            inlinePoliciesToDelete.addAll(inlinePolicyNames);

            LinkedHashSet<IamEntity> instanceProfiles = iamRoleResource.getInstanceProfiles();
            details.add("-- Instance profiles will be detached: " + instanceProfiles);
            instanceProfilesToDetach.addAll(instanceProfiles);

            LinkedHashSet<IamEntity> attachedPolicies = iamRoleResource.getAttachedPolicies();
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
            inlinePoliciesToDelete.forEach(p -> iamClient.deleteRolePolicy(new DeleteRolePolicyRequest().withRoleName(p.getRoleName()).withPolicyName(p.getEntityName())));
            instanceProfilesToDetach.forEach(p -> iamClient.removeRoleFromInstanceProfile(new RemoveRoleFromInstanceProfileRequest().withRoleName(p.getRoleName()).withInstanceProfileName(p.getEntityName())));
            policiesToDetach.forEach(p -> iamClient.detachRolePolicy(new DetachRolePolicyRequest().withRoleName(p.getRoleName()).withPolicyArn(p.getEntityName())));
            rolesToDelete.forEach(r -> iamClient.deleteRole(new DeleteRoleRequest().withRoleName(r)));
        }

        InterceptorRegistry.getAfterTerminateInterceptors()
                .forEach(interceptor -> interceptor.intercept(service, iamRoleResourceList, info.toString(), apply));

        LOGGER.info("Succeed.");
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
}
