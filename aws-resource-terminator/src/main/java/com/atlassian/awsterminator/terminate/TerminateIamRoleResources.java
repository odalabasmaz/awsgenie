package com.atlassian.awsterminator.terminate;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.identitymanagement.model.DeleteRoleRequest;
import com.atlassian.awsterminator.interceptor.InterceptorRegistry;
import com.atlassian.awstool.terminate.FetchResourceFactory;
import com.atlassian.awstool.terminate.FetchResources;
import com.atlassian.awstool.terminate.iamrole.IAMRoleResource;
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
public class TerminateIamRoleResources implements TerminateResources {
    private static final Logger LOGGER = LogManager.getLogger(TerminateIamRoleResources.class);

    private final AWSCredentialsProvider credentialsProvider;

    public TerminateIamRoleResources(AWSCredentialsProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
    }

    @Override
    public void terminateResource(String region,
                                  String service, List<String> resources, String ticket, boolean apply) throws Exception {

        AmazonIdentityManagement iamClient = AmazonIdentityManagementClient
                .builder()
                .withRegion(region)
                .withCredentials(credentialsProvider)
                .build();

        // Resources to be removed
        LinkedHashSet<String> rolesToDelete = new LinkedHashSet<>();

        List<String> details = new LinkedList<>();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date endDate = new Date();
        Date referenceDate = new Date(endDate.getTime() - TimeUnit.DAYS.toMillis(7));

        FetchResources fetcher = new FetchResourceFactory().getFetcher("iamRole", credentialsProvider);
        List<IAMRoleResource> iamRoleResourceList = (List<IAMRoleResource>) fetcher.fetchResources(region, resources, details);

        for (IAMRoleResource iamRoleResource : iamRoleResourceList) {
            if (iamRoleResource.getLastUsedDate() != null && iamRoleResource.getLastUsedDate().after(referenceDate)) {
                details.add("IAM role seems in use, not deleting: [" + iamRoleResource.getResourceName() + "], lastUsageDate: [" + sdf.format(referenceDate) + "]");
                LOGGER.warn("IAM role seems in use, not deleting: [" + iamRoleResource.getResourceName() + "], lastUsageDate: [" + sdf.format(referenceDate) + "]");
                continue;
            }
            details.add("IAM Role will be deleted: [" + details + "]");
            rolesToDelete.add(iamRoleResource.getResourceName());
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
            rolesToDelete.forEach(r -> iamClient.deleteRole(new DeleteRoleRequest().withRoleName(r)));
        }

        InterceptorRegistry.getAfterTerminateInterceptors()
                .forEach(interceptor -> interceptor.intercept(service, iamRoleResourceList, info.toString(), apply));

        LOGGER.info("Succeed.");
    }
}
