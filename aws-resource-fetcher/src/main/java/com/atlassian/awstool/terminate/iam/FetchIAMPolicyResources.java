package com.atlassian.awstool.terminate.iam;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.*;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import com.atlassian.awstool.terminate.FetchResources;
import com.atlassian.awstool.terminate.FetchResourcesWithProvider;
import com.atlassian.awstool.terminate.FetcherConfiguration;
import credentials.AwsClientProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author Orhun Dalabasmaz
 * @version 10.03.2021
 */

public class FetchIAMPolicyResources extends FetchResourcesWithProvider implements FetchResources<IAMPolicyResource> {
    private static final Logger LOGGER = LogManager.getLogger(FetchIAMPolicyResources.class);

    public FetchIAMPolicyResources(FetcherConfiguration configuration) {
        super(configuration);
    }

    @Override
    public Object getUsage(String region, String resourceArn) {
        AmazonIdentityManagement iamClient = AwsClientProvider.getInstance(getConfiguration()).getAmazonIAM();

        String jobId = iamClient.generateServiceLastAccessedDetails(new GenerateServiceLastAccessedDetailsRequest().withArn(resourceArn)).getJobId();
        List<ServiceLastAccessed> servicesLastAccessed = iamClient.getServiceLastAccessedDetails(new GetServiceLastAccessedDetailsRequest().withJobId(jobId)).getServicesLastAccessed();
        Optional<ServiceLastAccessed> latestUsageByAnyService = servicesLastAccessed.stream().max(Comparator.comparing(ServiceLastAccessed::getLastAuthenticated));

        if (latestUsageByAnyService.isPresent()
                && latestUsageByAnyService.get().getLastAuthenticated() != null) {
            return latestUsageByAnyService.get().getLastAuthenticated();
        }
        return null;
    }

    @Override
    public List<IAMPolicyResource> fetchResources(String region, List<String> resources, List<String> details) {

        AmazonIdentityManagement iamClient = AwsClientProvider.getInstance(getConfiguration()).getAmazonIAM();
        String accountId = AWSSecurityTokenServiceClientBuilder.standard().build()
                .getCallerIdentity(new GetCallerIdentityRequest()).getAccount();

        List<IAMPolicyResource> iamPolicyResourceList = new ArrayList<>();

        for (String policyName : resources) {
            try {
                String policyArn = generatePolicyArn(policyName, accountId);
                Policy policy = iamClient.getPolicy(new GetPolicyRequest().withPolicyArn(policyArn)).getPolicy();

                IAMPolicyResource iamPolicyResource = new IAMPolicyResource().setResourceName(policy.getArn());
                iamPolicyResourceList.add(iamPolicyResource);
            } catch (NoSuchEntityException ex) {
                details.add("!!! IAM Policy not exists: [" + policyName + "]");
                LOGGER.warn("!!! IAM Policy not exists: [" + policyName + "]");
            }
        }

        return iamPolicyResourceList;
    }

    @Override
    public void listResources(String region, Consumer<List<String>> consumer) {
        consume((nextMarker) -> {
            ListPoliciesResult listPoliciesResult = AwsClientProvider.getInstance(getConfiguration()).getAmazonIAM().listPolicies(new ListPoliciesRequest().withMarker(nextMarker));
            List<String> policyList = listPoliciesResult.getPolicies().stream().map(Policy::getPolicyName).collect(Collectors.toList());
            consumer.accept(policyList);
            return listPoliciesResult.getMarker();
        });
    }

    public static String generatePolicyArn(String policyName, String accountId) {
        return "arn:aws:iam::" + accountId + ":policy/" + policyName;
    }
}
