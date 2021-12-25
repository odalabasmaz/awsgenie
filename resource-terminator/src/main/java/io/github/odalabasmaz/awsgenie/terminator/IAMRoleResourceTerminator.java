package io.github.odalabasmaz.awsgenie.terminator;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.DeleteRolePolicyRequest;
import com.amazonaws.services.identitymanagement.model.DeleteRoleRequest;
import com.amazonaws.services.identitymanagement.model.DetachRolePolicyRequest;
import com.amazonaws.services.identitymanagement.model.RemoveRoleFromInstanceProfileRequest;
import io.github.odalabasmaz.awsgenie.fetcher.ResourceFetcher;
import io.github.odalabasmaz.awsgenie.fetcher.ResourceFetcherConfiguration;
import io.github.odalabasmaz.awsgenie.fetcher.ResourceFetcherFactory;
import io.github.odalabasmaz.awsgenie.fetcher.Service;
import io.github.odalabasmaz.awsgenie.fetcher.credentials.AWSClientConfiguration;
import io.github.odalabasmaz.awsgenie.fetcher.credentials.AWSClientProvider;
import io.github.odalabasmaz.awsgenie.fetcher.iam.IAMEntity;
import io.github.odalabasmaz.awsgenie.fetcher.iam.IAMRoleResource;
import io.github.odalabasmaz.awsgenie.terminator.configuration.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author Orhun Dalabasmaz
 * @version 06.04.2021
 * <p>
 * Details: Required permissions: iam:ListRoles, iam:DeleteRole
 * TODO: add more...
 */
public class IAMRoleResourceTerminator extends ResourceTerminator<IAMRoleResource> {
    private static final Logger LOGGER = LogManager.getLogger(IAMRoleResourceTerminator.class);

    public IAMRoleResourceTerminator(AWSClientConfiguration configuration) {
        super(configuration);
    }

    @Override
    public Set<IAMRoleResource> beforeApply(Configuration conf, boolean apply) throws Exception {
        return beforeApply(conf.getRegion(), Service.fromValue(conf.getService()), conf.getResourcesAsList(), conf.getDescription(),
                conf.getLastUsage(), conf.isForce(), apply);
    }

    @Override
    protected Set<IAMRoleResource> beforeApply(String region, Service service, List<String> resources, String ticket, boolean apply) throws Exception {
        return beforeApply(region, service, resources, ticket, 7, false, apply);
    }

    @Override
    protected Set<IAMRoleResource> beforeApply(String region, Service service, List<String> resources, String ticket, int lastUsage, boolean force, boolean apply) throws Exception {
        // Resources to be removed
        Set<IAMRoleResource> rolesToDelete = new LinkedHashSet<>();
        List<String> details = new LinkedList<>();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date endDate = new Date();
        Date referenceDate = new Date(endDate.getTime() - TimeUnit.DAYS.toMillis(lastUsage));

        ResourceFetcher<IAMRoleResource> fetcher = getFetchResourceFactory().getFetcher(service, new ResourceFetcherConfiguration(getConfiguration()));
        Set<IAMRoleResource> iamRoleResourceList = fetcher.fetchResources(region, resources, details);

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
            rolesToDelete.add(iamRoleResource);
        }

        StringBuilder info = new StringBuilder()
                .append("The resources will be terminated regarding ").append(ticket).append("\n")
                .append("* Dry-Run: ").append(!apply).append("\n")
                .append("* IAM roles: ").append(rolesToDelete).append("\n");

        info.append("Details:\n");
        details.forEach(d -> info.append("-- ").append(d).append("\n"));
        LOGGER.info(info);

        return rolesToDelete;
    }

    @Override
    protected void apply(Set<IAMRoleResource> resources, boolean apply) {
        List<String> details = new ArrayList<>();
        Set<String> rolesToDelete = new LinkedHashSet<>();
        Set<IAMEntity> inlinePoliciesToDelete = new LinkedHashSet<>();
        Set<IAMEntity> instanceProfilesToDetach = new LinkedHashSet<>();
        Set<IAMEntity> policiesToDetach = new LinkedHashSet<>();

        resources.forEach(resource -> {
            rolesToDelete.add(resource.getResourceName());

            LinkedHashSet<IAMEntity> inlinePolicyNames = resource.getInlinePolicies();
            details.add("-- Inline policies will be deleted: " + inlinePolicyNames);
            inlinePoliciesToDelete.addAll(inlinePolicyNames);

            LinkedHashSet<IAMEntity> instanceProfiles = resource.getInstanceProfiles();
            details.add("-- Instance profiles will be detached: " + instanceProfiles);
            instanceProfilesToDetach.addAll(instanceProfiles);

            LinkedHashSet<IAMEntity> attachedPolicies = resource.getAttachedPolicies();
            details.add("-- Attached profiles will be detached: " + attachedPolicies);
            policiesToDetach.addAll(attachedPolicies);
        });

        StringBuilder info = new StringBuilder();
        details.forEach(d -> info.append("-- ").append(d).append("\n"));
        LOGGER.info(info);

        if (!resources.isEmpty() && apply) {
            AmazonIdentityManagement iamClient = AWSClientProvider.getInstance(getConfiguration()).getAmazonIAM();

            LOGGER.info("Terminating the resources...");
            inlinePoliciesToDelete.forEach(p -> iamClient.deleteRolePolicy(new DeleteRolePolicyRequest().withRoleName(p.getRoleName()).withPolicyName(p.getEntityName())));
            instanceProfilesToDetach.forEach(p -> iamClient.removeRoleFromInstanceProfile(new RemoveRoleFromInstanceProfileRequest().withRoleName(p.getRoleName()).withInstanceProfileName(p.getEntityName())));
            policiesToDetach.forEach(p -> iamClient.detachRolePolicy(new DetachRolePolicyRequest().withRoleName(p.getRoleName()).withPolicyArn(p.getEntityName())));
            rolesToDelete.forEach(r -> iamClient.deleteRole(new DeleteRoleRequest().withRoleName(r)));
        }
    }

    @Override
    protected void afterApply(Set<IAMRoleResource> resources) {
        LOGGER.info("Succeed.");
    }

    void setFetchResourceFactory(ResourceFetcherFactory<IAMRoleResource> resourceFetcherFactory) {
        this.resourceFetcherFactory = resourceFetcherFactory;
    }
}
