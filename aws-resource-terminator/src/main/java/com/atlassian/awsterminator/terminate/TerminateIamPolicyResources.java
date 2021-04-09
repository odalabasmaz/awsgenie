package com.atlassian.awsterminator.terminate;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.identitymanagement.model.DeletePolicyRequest;
import com.atlassian.awsterminator.interceptor.InterceptorRegistry;
import com.atlassian.awstool.terminate.FetchResourceFactory;
import com.atlassian.awstool.terminate.FetchResources;
import com.atlassian.awstool.terminate.iamPolicy.IAMPolicyResource;
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
 */

public class TerminateIamPolicyResources implements TerminateResources {
    private static final Logger LOGGER = LogManager.getLogger(TerminateIamPolicyResources.class);

    private final AWSCredentialsProvider credentialsProvider;
    private AmazonIdentityManagement iamClient;
    private FetchResourceFactory fetchResourceFactory;

    public TerminateIamPolicyResources(AWSCredentialsProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
    }

    @Override
    public void terminateResource(String region,
                                  String service, List<String> resources, String ticket, boolean apply) throws Exception {

        AmazonIdentityManagement iamClient = getIAMClient(region);

        // Resources to be removed
        LinkedHashSet<String> policiesToDelete = new LinkedHashSet<>();

        List<String> details = new LinkedList<>();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date endDate = new Date();
        Date referenceDate = new Date(endDate.getTime() - TimeUnit.DAYS.toMillis(7));

        FetchResources fetcher = getFetchResourceFactory().getFetcher("iamPolicy", credentialsProvider);
        List<IAMPolicyResource> iamPolicyResourceList = (List<IAMPolicyResource>) fetcher.fetchResources(region, resources, details);

        for (IAMPolicyResource iamPolicyResource : iamPolicyResourceList) {
            if (iamPolicyResource.getLastUsedDate() != null && iamPolicyResource.getLastUsedDate().after(referenceDate)) {
                details.add("IAM policy seems in use, not deleting: [" + iamPolicyResource.getResourceName() + "], lastUsageDate: [" + sdf.format(iamPolicyResource.getLastUsedDate()) + "]");
                LOGGER.warn("IAM policy seems in use, not deleting: [" + iamPolicyResource.getResourceName() + "], lastUsageDate: [" + sdf.format(iamPolicyResource.getLastUsedDate()) + "]");
                continue;
            }
            policiesToDelete.add(iamPolicyResource.getResourceName());
        }

        StringBuilder info = new StringBuilder()
                .append("The resources will be terminated regarding ").append(ticket).append("\n")
                .append("* Dry-Run: ").append(!apply).append("\n")
                .append("* IAM Policies: ").append(policiesToDelete).append("\n");

        info.append("Details:\n");
        details.forEach(d -> info.append("-- ").append(d).append("\n"));
        LOGGER.info(info);

        InterceptorRegistry.getBeforeTerminateInterceptors()
                .forEach(interceptor -> interceptor.intercept(service, iamPolicyResourceList, info.toString(), apply));

        if (apply) {
            LOGGER.info("Terminating the resources...");
            policiesToDelete.forEach(r -> iamClient.deletePolicy(new DeletePolicyRequest().withPolicyArn(r)));
        }

        InterceptorRegistry.getAfterTerminateInterceptors()
                .forEach(interceptor -> interceptor.intercept(service, iamPolicyResourceList, info.toString(), apply));

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

    void setFetchResourceFactory(FetchResourceFactory fetchResourceFactory) {
        this.fetchResourceFactory = fetchResourceFactory;
    }

    private FetchResourceFactory getFetchResourceFactory() {
        if (this.fetchResourceFactory != null) {
            return this.fetchResourceFactory;
        } else {
            return new FetchResourceFactory();
        }
    }
}
