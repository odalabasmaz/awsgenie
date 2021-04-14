package io.github.odalabasmaz.awsgenie.terminator;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.DeletePolicyRequest;
import io.github.odalabasmaz.awsgenie.fetcher.ResourceFetcher;
import io.github.odalabasmaz.awsgenie.fetcher.ResourceFetcherConfiguration;
import io.github.odalabasmaz.awsgenie.fetcher.ResourceFetcherFactory;
import io.github.odalabasmaz.awsgenie.fetcher.Service;
import io.github.odalabasmaz.awsgenie.fetcher.credentials.AWSClientConfiguration;
import io.github.odalabasmaz.awsgenie.fetcher.credentials.AWSClientProvider;
import io.github.odalabasmaz.awsgenie.fetcher.iam.IAMPolicyResource;
import io.github.odalabasmaz.awsgenie.terminator.configuration.Configuration;
import io.github.odalabasmaz.awsgenie.terminator.interceptor.InterceptorRegistry;
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

public class IAMPolicyResourceTerminator extends ResourceTerminatorWithProvider implements ResourceTerminator<IAMPolicyResource> {
    private static final Logger LOGGER = LogManager.getLogger(IAMPolicyResourceTerminator.class);

    private ResourceFetcherFactory<IAMPolicyResource> resourceFetcherFactory;

    public IAMPolicyResourceTerminator(AWSClientConfiguration configuration) {
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
        LinkedHashSet<String> policiesToDelete = new LinkedHashSet<>();

        List<String> details = new LinkedList<>();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date endDate = new Date();
        Date referenceDate = new Date(endDate.getTime() - TimeUnit.DAYS.toMillis(lastUsage));

        ResourceFetcher<IAMPolicyResource> fetcher = getFetchResourceFactory().getFetcher(service, new ResourceFetcherConfiguration(getConfiguration()));
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

    void setFetchResourceFactory(ResourceFetcherFactory<IAMPolicyResource> resourceFetcherFactory) {
        this.resourceFetcherFactory = resourceFetcherFactory;
    }

    //TODO: we can centralize these functions which are same for all services..
    private ResourceFetcherFactory<IAMPolicyResource> getFetchResourceFactory() {
        if (this.resourceFetcherFactory != null) {
            return this.resourceFetcherFactory;
        } else {
            return new ResourceFetcherFactory<>();
        }
    }
}
