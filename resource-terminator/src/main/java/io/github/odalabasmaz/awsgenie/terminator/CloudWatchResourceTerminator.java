package io.github.odalabasmaz.awsgenie.terminator;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.DeleteAlarmsRequest;
import io.github.odalabasmaz.awsgenie.fetcher.ResourceFetcher;
import io.github.odalabasmaz.awsgenie.fetcher.ResourceFetcherConfiguration;
import io.github.odalabasmaz.awsgenie.fetcher.ResourceFetcherFactory;
import io.github.odalabasmaz.awsgenie.fetcher.Service;
import io.github.odalabasmaz.awsgenie.fetcher.cloudwatch.CloudWatchResource;
import io.github.odalabasmaz.awsgenie.fetcher.credentials.AWSClientConfiguration;
import io.github.odalabasmaz.awsgenie.fetcher.credentials.AWSClientProvider;
import io.github.odalabasmaz.awsgenie.terminator.configuration.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Orhun Dalabasmaz
 * @version 10.03.2021
 */

public class CloudWatchResourceTerminator extends ResourceTerminator<CloudWatchResource> {
    private static final Logger LOGGER = LogManager.getLogger(CloudWatchResourceTerminator.class);

    public CloudWatchResourceTerminator(AWSClientConfiguration configuration) {
        super(configuration);
    }

    @Override
    public Set<CloudWatchResource> beforeApply(Configuration conf, boolean apply) throws Exception {
        return beforeApply(conf.getRegion(), Service.fromValue(conf.getService()), conf.getResourcesAsList(), conf.getDescription(),
                conf.getLastUsage(), conf.isForce(), apply);
    }

    @Override
    protected Set<CloudWatchResource> beforeApply(String region, Service service, List<String> resources, String ticket, boolean apply) throws Exception {
        return beforeApply(region, service, resources, ticket, 7, false, apply);
    }

    @Override
    protected Set<CloudWatchResource> beforeApply(String region, Service service, List<String> resources, String ticket, int lastUsage, boolean force, boolean apply) throws Exception {
        ResourceFetcher<CloudWatchResource> fetcher = getFetchResourceFactory().getFetcher(service, new ResourceFetcherConfiguration(getConfiguration()));
        Set<CloudWatchResource> cloudWatchResourceList = fetcher.fetchResources(region, resources, null);

        Set<String> cloudwatchAlarmsToDelete = new HashSet<>();
        Set<String> cloudwatchAlarmsNotToDelete = new HashSet<>(resources);
        for (CloudWatchResource cloudwatchResource : cloudWatchResourceList) {
            String alarmName = cloudwatchResource.getResourceName();
            cloudwatchAlarmsNotToDelete.remove(alarmName);
            cloudwatchAlarmsToDelete.add(alarmName);
        }

        StringBuilder info = new StringBuilder()
                .append("The resources will be terminated regarding ").append(ticket).append("\n")
                .append("* Dry-Run: ").append(!apply).append("\n")
                .append("* Cloudwatch alarms to be deleted: ").append(cloudwatchAlarmsToDelete).append("\n")
                .append("* Cloudwatch alarms not found: ").append(cloudwatchAlarmsNotToDelete).append("\n");
        LOGGER.info(info);

        return cloudWatchResourceList;
    }

    @Override
    protected void apply(Set<CloudWatchResource> resources, boolean apply) {
        if (!resources.isEmpty() && apply) {
            AmazonCloudWatch cloudWatchClient = AWSClientProvider.getInstance(getConfiguration()).getAmazonCloudWatch();
            LOGGER.info("Terminating the resources...");

            List<String> resourcesToDelete = resources
                    .stream()
                    .map(CloudWatchResource::getResourceName)
                    .collect(Collectors.toList());
            cloudWatchClient.deleteAlarms(new DeleteAlarmsRequest().withAlarmNames(resourcesToDelete));
        }
    }

    @Override
    protected void afterApply(Set<CloudWatchResource> resources) {
        LOGGER.info("Succeed.");
    }

    void setFetchResourceFactory(ResourceFetcherFactory<CloudWatchResource> resourceFetcherFactory) {
        this.resourceFetcherFactory = resourceFetcherFactory;
    }
}
