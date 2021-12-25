package io.github.odalabasmaz.awsgenie.terminator;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.DeleteAlarmsRequest;
import com.amazonaws.services.kinesis.AmazonKinesis;
import io.github.odalabasmaz.awsgenie.fetcher.ResourceFetcher;
import io.github.odalabasmaz.awsgenie.fetcher.ResourceFetcherConfiguration;
import io.github.odalabasmaz.awsgenie.fetcher.ResourceFetcherFactory;
import io.github.odalabasmaz.awsgenie.fetcher.Service;
import io.github.odalabasmaz.awsgenie.fetcher.credentials.AWSClientConfiguration;
import io.github.odalabasmaz.awsgenie.fetcher.credentials.AWSClientProvider;
import io.github.odalabasmaz.awsgenie.fetcher.kinesis.KinesisResource;
import io.github.odalabasmaz.awsgenie.terminator.configuration.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Celal Emre CICEK
 * @version 7.04.2021
 */

public class KinesisResourceTerminator extends ResourceTerminator<KinesisResource> {
    private static final Logger LOGGER = LogManager.getLogger(KinesisResourceTerminator.class);

    public KinesisResourceTerminator(AWSClientConfiguration configuration) {
        super(configuration);
    }

    @Override
    public Set<KinesisResource> beforeApply(Configuration conf, boolean apply) throws Exception {
        return beforeApply(conf.getRegion(), Service.fromValue(conf.getService()), conf.getResourcesAsList(), conf.getDescription(),
                conf.getLastUsage(), conf.isForce(), apply);
    }

    @Override
    protected Set<KinesisResource> beforeApply(String region, Service service, List<String> resources, String ticket, boolean apply) throws Exception {
        return beforeApply(region, service, resources, ticket, 7, false, apply);
    }

    @Override
    protected Set<KinesisResource> beforeApply(String region, Service service, List<String> resources, String ticket, int lastUsage, boolean force, boolean apply) throws Exception {
        // Resources to be removed
        Set<KinesisResource> streamsToDelete = new LinkedHashSet<>();
        Set<String> cloudwatchAlarmsToDelete = new LinkedHashSet<>();
        List<String> details = new LinkedList<>();

        ResourceFetcher<KinesisResource> fetcher = getFetchResourceFactory().getFetcher(service, new ResourceFetcherConfiguration(getConfiguration()));
        Set<KinesisResource> kinesisResourceList = fetcher.fetchResources(region, resources, details);

        for (KinesisResource kinesisResource : kinesisResourceList) {
            String streamName = kinesisResource.getResourceName();
            Double totalUsage = (Double) fetcher.getUsage(region, streamName, lastUsage);
            if (totalUsage > 0) {
                if (force) {
                    details.add("Kinesis stream seems in use, but still deleting with force: [" + streamName + "], totalUsage: [" + totalUsage + "]");
                    LOGGER.warn("Kinesis stream seems in use, but still deleting with force: [" + streamName + "], totalUsage: [" + totalUsage + "]");
                } else {
                    details.add("Kinesis stream seems in use, not deleting: [" + streamName + "], totalUsage: [" + totalUsage + "]");
                    LOGGER.warn("Kinesis stream seems in use, not deleting: [" + streamName + "], totalUsage: [" + totalUsage + "]");
                    continue;
                }
            }

            streamsToDelete.add(kinesisResource);
            cloudwatchAlarmsToDelete.addAll(kinesisResource.getCloudwatchAlarmList());
        }

        StringBuilder info = new StringBuilder()
                .append("The resources will be terminated regarding ").append(ticket).append("\n")
                .append("* Dry-Run: ").append(!apply).append("\n")
                .append("* Kinesis streams: ").append(streamsToDelete).append("\n")
                .append("* Cloudwatch alarms: ").append(cloudwatchAlarmsToDelete).append("\n");

        info.append("Details:\n");
        details.forEach(d -> info.append("-- ").append(d).append("\n"));
        LOGGER.info(info);

        return streamsToDelete;
    }

    @Override
    protected void apply(Set<KinesisResource> resources, boolean apply) {
        if (!resources.isEmpty() && apply) {
            AmazonKinesis kinesisClient = AWSClientProvider.getInstance(getConfiguration()).getAmazonKinesis();
            AmazonCloudWatch cloudWatchClient = AWSClientProvider.getInstance(getConfiguration()).getAmazonCloudWatch();
            HashSet<String> cloudwatchAlarmsToDelete = new LinkedHashSet<>();

            Set<String> streamsToDelete = resources
                    .stream()
                    .map(KinesisResource::getResourceName)
                    .collect(Collectors.toSet());
            resources
                    .stream()
                    .map(KinesisResource::getCloudwatchAlarmList)
                    .forEach(cloudwatchAlarmsToDelete::addAll);

            LOGGER.info("Terminating the resources...");

            streamsToDelete.forEach(kinesisClient::deleteStream);

            if (!cloudwatchAlarmsToDelete.isEmpty()) {
                cloudWatchClient.deleteAlarms(new DeleteAlarmsRequest().withAlarmNames(cloudwatchAlarmsToDelete));
            }
        }
    }

    @Override
    protected void afterApply(Set<KinesisResource> resources) {
        LOGGER.info("Succeed.");
    }

    void setFetchResourceFactory(ResourceFetcherFactory<KinesisResource> resourceFetcherFactory) {
        this.resourceFetcherFactory = resourceFetcherFactory;
    }
}
