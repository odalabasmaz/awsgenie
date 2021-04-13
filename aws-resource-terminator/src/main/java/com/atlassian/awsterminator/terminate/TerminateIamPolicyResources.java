package com.atlassian.awsterminator.terminate;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.DeletePolicyRequest;
import com.atlassian.awsterminator.configuration.Configuration;
import com.atlassian.awsterminator.interceptor.InterceptorRegistry;
import com.atlassian.awstool.terminate.FetchResourceFactory;
import com.atlassian.awstool.terminate.FetchResources;
import com.atlassian.awstool.terminate.FetcherConfiguration;
import com.atlassian.awstool.terminate.Service;
import com.atlassian.awstool.terminate.iam.IAMPolicyResource;
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
 */

public class TerminateIamPolicyResources extends TerminateResourcesWithProvider implements TerminateResources<IAMPolicyResource> {
    private static final Logger LOGGER = LogManager.getLogger(TerminateIamPolicyResources.class);

    private FetchResourceFactory<IAMPolicyResource> fetchResourceFactory;

    public TerminateIamPolicyResources(AwsClientConfiguration configuration) {
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

        AmazonIdentityManagement iamClient = AwsClientProvider.getInstance(getConfiguration()).getAmazonIAM();

        // Resources to be removed
        LinkedHashSet<String> policiesToDelete = new LinkedHashSet<>();

        List<String> details = new LinkedList<>();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date endDate = new Date();
        Date referenceDate = new Date(endDate.getTime() - TimeUnit.DAYS.toMillis(lastUsage));

        FetchResources<IAMPolicyResource> fetcher = getFetchResourceFactory().getFetcher(service, new FetcherConfiguration(getConfiguration()));
        List<IAMPolicyResource> iamPolicyResourceList = fetcher.fetchResources(region, resources, details);

        for (IAMPolicyResource iamPolicyResource : iamPolicyResourceList) {
            String policyName = iamPolicyResource.getResourceName();
            //TODO: fetching later, why not fetched at first...
            Date lastUsedDate = (Date) fetcher.getUsage(region, policyName, lastUsage);
            if (lastUsedDate != null && lastUsedDate.after(referenceDate)) {
                if (force) {
                    details.add("IAM policy seems in use, but still deleting with force: [" + policyName + "], lastUsageDate: [" + sdf.format(lastUsedDate) + "]");
                    LOGGER.warn("IAM policy seems in use, but still deleting with force: [" + policyName + "], lastUsageDate: [" + sdf.format(lastUsedDate) + "]");
                } else {
                    details.add("IAM policy seems in use, not deleting: [" + policyName + "], lastUsageDate: [" + sdf.format(lastUsedDate) + "]");
                    LOGGER.warn("IAM policy seems in use, not deleting: [" + policyName + "], lastUsageDate: [" + sdf.format(lastUsedDate) + "]");
                    continue;
                }
            }
            policiesToDelete.add(policyName);
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

    void setFetchResourceFactory(FetchResourceFactory<IAMPolicyResource> fetchResourceFactory) {
        this.fetchResourceFactory = fetchResourceFactory;
    }

    //TODO: we can centralize these functions which are same for all services..
    private FetchResourceFactory<IAMPolicyResource> getFetchResourceFactory() {
        if (this.fetchResourceFactory != null) {
            return this.fetchResourceFactory;
        } else {
            return new FetchResourceFactory<>();
        }
    }
}
