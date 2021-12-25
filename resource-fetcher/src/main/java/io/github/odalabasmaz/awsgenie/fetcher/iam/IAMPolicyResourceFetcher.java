package io.github.odalabasmaz.awsgenie.fetcher.iam;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.*;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import io.github.odalabasmaz.awsgenie.fetcher.ResourceFetcher;
import io.github.odalabasmaz.awsgenie.fetcher.ResourceFetcherConfiguration;
import io.github.odalabasmaz.awsgenie.fetcher.ResourceFetcherWithProvider;
import io.github.odalabasmaz.awsgenie.fetcher.credentials.AWSClientProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author Orhun Dalabasmaz
 * @version 10.03.2021
 */

public class IAMPolicyResourceFetcher extends ResourceFetcherWithProvider implements ResourceFetcher<IAMPolicyResource> {
    private static final Logger LOGGER = LogManager.getLogger(IAMPolicyResourceFetcher.class);

    public IAMPolicyResourceFetcher(ResourceFetcherConfiguration configuration) {
        super(configuration);
    }

    @Override
    public Object getUsage(String region, String resourceArn, int lastDays) {
        AmazonIdentityManagement iamClient = AWSClientProvider.getInstance(getConfiguration()).getAmazonIAM();

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
    public Set<IAMPolicyResource> fetchResources(String region, List<String> resources, List<String> details) {

        AmazonIdentityManagement iamClient = AWSClientProvider.getInstance(getConfiguration()).getAmazonIAM();
        String accountId = AWSSecurityTokenServiceClientBuilder.standard().build()
                .getCallerIdentity(new GetCallerIdentityRequest()).getAccount();

        Set<IAMPolicyResource> iamPolicyResourceList = new LinkedHashSet<>();

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
            ListPoliciesResult listPoliciesResult = AWSClientProvider.getInstance(getConfiguration()).getAmazonIAM().listPolicies(new ListPoliciesRequest().withMarker(nextMarker));
            List<String> policyList = listPoliciesResult.getPolicies().stream().map(Policy::getPolicyName).collect(Collectors.toList());
            consumer.accept(policyList);
            return listPoliciesResult.getMarker();
        });
    }

    public static String generatePolicyArn(String policyName, String accountId) {
        return "arn:aws:iam::" + accountId + ":policy/" + policyName;
    }
}
