package com.atlassian.awstool.terminate.iamPolicy;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.identitymanagement.model.*;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import com.atlassian.awstool.terminate.AWSResource;
import com.atlassian.awstool.terminate.FetchResources;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * @author Orhun Dalabasmaz
 * @version 10.03.2021
 */

public class FetchIAMPolicyResources implements FetchResources {
    private static final Logger LOGGER = LogManager.getLogger(FetchIAMPolicyResources.class);

    private final AWSCredentialsProvider credentialsProvider;

    public FetchIAMPolicyResources(AWSCredentialsProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
    }

    @Override
    public List<? extends AWSResource> fetchResources(String region, String service, List<String> resources, List<String> details) {

        AmazonIdentityManagement iamClient = AmazonIdentityManagementClient
                .builder()
                .withRegion(region)
                .withCredentials(credentialsProvider)
                .build();
        String accountId = AWSSecurityTokenServiceClientBuilder.standard().build()
                .getCallerIdentity(new GetCallerIdentityRequest()).getAccount();

        List<IAMPolicyResource> iamPolicyResourceList = new ArrayList<>();

        for (String policyName : resources) {
            try {
                String policyArn = generatePolicyArn(policyName, accountId);
                Policy policy = iamClient.getPolicy(new GetPolicyRequest().withPolicyArn(policyArn)).getPolicy();

                IAMPolicyResource iamPolicyResource = new IAMPolicyResource().setResourceName(policy.getArn());

                String jobId = iamClient.generateServiceLastAccessedDetails(new GenerateServiceLastAccessedDetailsRequest().withArn(policyArn)).getJobId();
                List<ServiceLastAccessed> servicesLastAccessed = iamClient.getServiceLastAccessedDetails(new GetServiceLastAccessedDetailsRequest().withJobId(jobId)).getServicesLastAccessed();
                Optional<ServiceLastAccessed> latestUsageByAnyService = servicesLastAccessed.stream().max(Comparator.comparing(ServiceLastAccessed::getLastAuthenticated));

                if (latestUsageByAnyService.isPresent()
                        && latestUsageByAnyService.get().getLastAuthenticated() != null) {
                    iamPolicyResource.setLastUsedDate(latestUsageByAnyService.get().getLastAuthenticated());
                }

                iamPolicyResourceList.add(iamPolicyResource);
            } catch (NoSuchEntityException ex) {
                details.add("!!! IAM Policy not exists: [" + policyName + "]");
                LOGGER.warn("!!! IAM Policy not exists: [" + policyName + "]");
            }
        }

        return iamPolicyResourceList;
    }

    public static String generatePolicyArn(String policyName, String accountId) {
        return "arn:aws:iam::" + accountId + ":policy/" + policyName;
    }
}
