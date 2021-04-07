package com.atlassian.awsterminator.terminate;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.identitymanagement.model.DeletePolicyRequest;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
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

    private String accountId;

    public TerminateIamPolicyResources(AWSCredentialsProvider credentialsProvider) {
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
        this.accountId = AWSSecurityTokenServiceClientBuilder.standard().build()
                .getCallerIdentity(new GetCallerIdentityRequest()).getAccount();


        // Resources to be removed
        LinkedHashSet<String> policiesToDelete = new LinkedHashSet<>();

        List<String> details = new LinkedList<>();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date endDate = new Date();
        Date referenceDate = new Date(endDate.getTime() - TimeUnit.DAYS.toMillis(7));

        /*for (String policyName : resources) {
            try {
                String policyArn = generatePolicyArn(policyName);
                Policy policy = iamClient.getPolicy(new GetPolicyRequest().withPolicyArn(policyArn)).getPolicy();

                String jobId = iamClient.generateServiceLastAccessedDetails(new GenerateServiceLastAccessedDetailsRequest().withArn(policyArn)).getJobId();
                List<ServiceLastAccessed> servicesLastAccessed = iamClient.getServiceLastAccessedDetails(new GetServiceLastAccessedDetailsRequest().withJobId(jobId)).getServicesLastAccessed();
                Optional<ServiceLastAccessed> latestUsageByAnyService = servicesLastAccessed.stream().max(Comparator.comparing(ServiceLastAccessed::getLastAuthenticated));

                if (latestUsageByAnyService.isPresent()
                        && latestUsageByAnyService.get().getLastAuthenticated() != null
                        && latestUsageByAnyService.get().getLastAuthenticated().after(referenceDate)) {
                    details.add("IAM policy seems in use, not deleting: [" + policyName + "], lastUsageDate: [" + sdf.format(referenceDate) + "]");
                    LOGGER.warn("IAM policy seems in use, not deleting: [" + policyName + "], lastUsageDate: [" + sdf.format(referenceDate) + "]");
                    continue;
                }

                details.add("IAM Policy will be deleted: [" + policyName + "]");
                policiesToDelete.add(policy.getArn());
            } catch (NoSuchEntityException ex) {
                details.add("!!! IAM Policy not exists: [" + policyName + "]");
                LOGGER.warn("!!! IAM Policy not exists: [" + policyName + "]");
            }
        }*/

        FetchResources fetcher = new FetchResourceFactory().getFetcher("iamPolicy", credentialsProvider);
        List<IAMPolicyResource> iamPolicyResourceList = (List<IAMPolicyResource>) fetcher.fetchResources(region, service, resources, details);

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

        if (apply) {
            LOGGER.info("Terminating the resources...");
            policiesToDelete.forEach(r -> iamClient.deletePolicy(new DeletePolicyRequest().withPolicyArn(r)));
        }

        LOGGER.info("Succeed.");
    }

    private String generatePolicyArn(String policyName) {
        return "arn:aws:iam::" + accountId + ":policy/" + policyName;
    }
}
