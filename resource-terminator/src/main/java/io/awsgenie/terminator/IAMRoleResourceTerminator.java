package io.awsgenie.terminator;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.DeleteRolePolicyRequest;
import com.amazonaws.services.identitymanagement.model.DeleteRoleRequest;
import com.amazonaws.services.identitymanagement.model.DetachRolePolicyRequest;
import com.amazonaws.services.identitymanagement.model.RemoveRoleFromInstanceProfileRequest;
import io.awsgenie.fetcher.ResourceFetcher;
import io.awsgenie.fetcher.ResourceFetcherConfiguration;
import io.awsgenie.fetcher.ResourceFetcherFactory;
import io.awsgenie.fetcher.Service;
import io.awsgenie.fetcher.credentials.AWSClientConfiguration;
import io.awsgenie.fetcher.credentials.AWSClientProvider;
import io.awsgenie.fetcher.iam.IAMEntity;
import io.awsgenie.fetcher.iam.IAMRoleResource;
import io.awsgenie.terminator.configuration.Configuration;
import io.awsgenie.terminator.interceptor.InterceptorRegistry;
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
public class IAMRoleResourceTerminator extends ResourceTerminatorWithProvider implements ResourceTerminator<IAMRoleResource> {
    private static final Logger LOGGER = LogManager.getLogger(IAMRoleResourceTerminator.class);

    private ResourceFetcherFactory<IAMRoleResource> resourceFetcherFactory;

    public IAMRoleResourceTerminator(AWSClientConfiguration configuration) {
        super(configuration);
    }

    @Override
    public void terminateResource(Configuration conf, boolean apply) throws Exception {
        terminateResource(conf.getRegion(), Service.fromValue(conf.getService()), conf.getResourcesAsList(), conf.getDescription(),
                conf.getLastUsage(), conf.isForce(), apply);
    }

    @Override
    public void terminateResource(String region, Service service, List<String> resources, String ticket, boolean apply) throws Exception {
        terminateResource(region, service, resources, ticket, 7, false, apply);
    }

    @Override
    public void terminateResource(String region, Service service, List<String> resources, String ticket, int lastUsage, boolean force, boolean apply) throws Exception {
        AmazonIdentityManagement iamClient = AWSClientProvider.getInstance(getConfiguration()).getAmazonIAM();

        // Resources to be removed
        LinkedHashSet<String> rolesToDelete = new LinkedHashSet<>();
        LinkedHashSet<IAMEntity> inlinePoliciesToDelete = new LinkedHashSet<>();
        LinkedHashSet<IAMEntity> instanceProfilesToDetach = new LinkedHashSet<>();
        LinkedHashSet<IAMEntity> policiesToDetach = new LinkedHashSet<>();
        List<String> details = new LinkedList<>();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date endDate = new Date();
        Date referenceDate = new Date(endDate.getTime() - TimeUnit.DAYS.toMillis(lastUsage)); //TODO: make this configurable...

        ResourceFetcher<IAMRoleResource> fetcher = getFetchResourceFactory().getFetcher(service, new ResourceFetcherConfiguration(getConfiguration()));
        List<IAMRoleResource> iamRoleResourceList = fetcher.fetchResources(resources, details);

        for (IAMRoleResource iamRoleResource : iamRoleResourceList) {
            String roleName = iamRoleResource.getResourceName();
            Date lastUsedDate = (Date) fetcher.getUsage(region, roleName, lastUsage);
            if (lastUsedDate != null && lastUsedDate.after(referenceDate)) {
                if (force) {
                    details.add("IAM role seems in use, but still deleting with force: [" + roleName + "], lastUsageDate: [" + sdf.format(lastUsedDate) + "]");
                    LOGGER.warn("IAM role seems in use, but still deleting with force: [" + roleName + "], lastUsageDate: [" + sdf.format(lastUsedDate) + "]");
                } else {
                    details.add("IAM role seems in use, not deleting: [" + roleName + "], lastUsageDate: [" + sdf.format(lastUsedDate) + "]");
                    LOGGER.warn("IAM role seems in use, not deleting: [" + roleName + "], lastUsageDate: [" + sdf.format(lastUsedDate) + "]");
                    continue;
                }
            }
            details.add("IAM Role will be deleted: " + roleName);
            rolesToDelete.add(roleName);

            LinkedHashSet<IAMEntity> inlinePolicyNames = iamRoleResource.getInlinePolicies();
            details.add("-- Inline policies will be deleted: " + inlinePolicyNames);
            inlinePoliciesToDelete.addAll(inlinePolicyNames);

            LinkedHashSet<IAMEntity> instanceProfiles = iamRoleResource.getInstanceProfiles();
            details.add("-- Instance profiles will be detached: " + instanceProfiles);
            instanceProfilesToDetach.addAll(instanceProfiles);

            LinkedHashSet<IAMEntity> attachedPolicies = iamRoleResource.getAttachedPolicies();
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

    void setFetchResourceFactory(ResourceFetcherFactory<IAMRoleResource> resourceFetcherFactory) {
        this.resourceFetcherFactory = resourceFetcherFactory;
    }

    private ResourceFetcherFactory<IAMRoleResource> getFetchResourceFactory() {
        if (this.resourceFetcherFactory != null) {
            return this.resourceFetcherFactory;
        } else {
            return new ResourceFetcherFactory<>();
        }
    }
}
