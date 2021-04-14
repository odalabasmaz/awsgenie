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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Orhun Dalabasmaz
 * @version 06.04.2021
 */

public class IAMPolicyResourceTerminator extends ResourceTerminator<IAMPolicyResource> {
    private static final Logger LOGGER = LogManager.getLogger(IAMPolicyResourceTerminator.class);

    private ResourceFetcherFactory<IAMPolicyResource> resourceFetcherFactory;

    public IAMPolicyResourceTerminator(AWSClientConfiguration configuration) {
        super(configuration);
    }

    @Override
    public Set<IAMPolicyResource> beforeApply(Configuration conf, boolean apply) throws Exception {
        return beforeApply(conf.getRegion(), Service.fromValue(conf.getService()), conf.getResourcesAsList(), conf.getDescription(),
                conf.getLastUsage(), conf.isForce(), apply);
    }

    @Override
    protected Set<IAMPolicyResource> beforeApply(String region, Service service, List<String> resources, String ticket, boolean apply) throws Exception {
        return beforeApply(region, service, resources, ticket, 7, false, apply);
    }

    @Override
    protected Set<IAMPolicyResource> beforeApply(String region, Service service, List<String> resources, String ticket, int lastUsage, boolean force, boolean apply) throws Exception {
        // Resources to be removed
        Set<IAMPolicyResource> policiesToDelete = new LinkedHashSet<>();
        List<String> details = new LinkedList<>();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date endDate = new Date();
        Date referenceDate = new Date(endDate.getTime() - TimeUnit.DAYS.toMillis(lastUsage));

        ResourceFetcher<IAMPolicyResource> fetcher = getFetchResourceFactory().getFetcher(service, new ResourceFetcherConfiguration(getConfiguration()));
        Set<IAMPolicyResource> iamPolicyResourceList = fetcher.fetchResources(region, resources, details);

        for (IAMPolicyResource iamPolicyResource : iamPolicyResourceList) {
            String policyName = iamPolicyResource.getResourceName();
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

            policiesToDelete.add(iamPolicyResource);
        }

        StringBuilder info = new StringBuilder()
                .append("The resources will be terminated regarding ").append(ticket).append("\n")
                .append("* Dry-Run: ").append(!apply).append("\n")
                .append("* IAM Policies: ").append(policiesToDelete).append("\n");

        info.append("Details:\n");
        details.forEach(d -> info.append("-- ").append(d).append("\n"));
        LOGGER.info(info);

        return policiesToDelete;
    }

    @Override
    protected void apply(Set<IAMPolicyResource> resources, boolean apply) {
        if (!resources.isEmpty() && apply) {
            AmazonIdentityManagement iamClient = AWSClientProvider.getInstance(getConfiguration()).getAmazonIAM();

            List<String> policiesToDelete = resources
                    .stream()
                    .map(IAMPolicyResource::getResourceName)
                    .collect(Collectors.toList());

            LOGGER.info("Terminating the resources...");
            policiesToDelete.forEach(r -> iamClient.deletePolicy(new DeletePolicyRequest().withPolicyArn(r)));
        }
    }

    @Override
    protected void afterApply(Set<IAMPolicyResource> resources) {
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
