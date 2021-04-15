package io.github.odalabasmaz.awsgenie.terminator;

import com.amazonaws.arn.Arn;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.DeleteAlarmsRequest;
import com.amazonaws.services.sns.AmazonSNS;
import io.github.odalabasmaz.awsgenie.fetcher.ResourceFetcher;
import io.github.odalabasmaz.awsgenie.fetcher.ResourceFetcherConfiguration;
import io.github.odalabasmaz.awsgenie.fetcher.ResourceFetcherFactory;
import io.github.odalabasmaz.awsgenie.fetcher.Service;
import io.github.odalabasmaz.awsgenie.fetcher.credentials.AWSClientConfiguration;
import io.github.odalabasmaz.awsgenie.fetcher.credentials.AWSClientProvider;
import io.github.odalabasmaz.awsgenie.fetcher.sns.SNSResource;
import io.github.odalabasmaz.awsgenie.terminator.configuration.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Celal Emre CICEK
 * @version 6.04.2021
 */

public class SNSResourceTerminator extends ResourceTerminator<SNSResource> {
    private static final Logger LOGGER = LogManager.getLogger(SNSResourceTerminator.class);

    private ResourceFetcherFactory<SNSResource> resourceFetcherFactory;

    public SNSResourceTerminator(AWSClientConfiguration configuration) {
        super(configuration);
    }

    @Override
    public Set<SNSResource> beforeApply(Configuration conf, boolean apply) throws Exception {
        return beforeApply(conf.getRegion(), Service.fromValue(conf.getService()), conf.getResourcesAsList(), conf.getDescription(),
                conf.getLastUsage(), conf.isForce(), apply);
    }

    @Override
    protected Set<SNSResource> beforeApply(String region, Service service, List<String> resources, String ticket, boolean apply) throws Exception {
        return beforeApply(region, service, resources, ticket, 7, false, apply);
    }

    @Override
    protected Set<SNSResource> beforeApply(String region, Service service, List<String> resources, String ticket, int lastUsage, boolean force, boolean apply) throws Exception {
        Set<SNSResource> topicsToDelete = new LinkedHashSet<>();
        Set<String> cloudwatchAlarmsToDelete = new LinkedHashSet<>();
        List<String> details = new LinkedList<>();

        ResourceFetcher<SNSResource> fetcher = getFetchResourceFactory().getFetcher(service, new ResourceFetcherConfiguration(getConfiguration()));
        Set<SNSResource> snsResourceList = fetcher.fetchResources(region, resources, details);

        for (SNSResource snsResource : snsResourceList) {
            String topicName = snsResource.getResourceName();
            Double publishCountInLastWeek = (Double) fetcher.getUsage(region, topicName, lastUsage);
            if (publishCountInLastWeek > 0) {
                if (force) {
                    details.add("Topic seems in use, but still deleting with force: [" + topicName + "], totalUsage: [" + publishCountInLastWeek + "]");
                    LOGGER.warn("Topic seems in use, but still deleting with force: [" + topicName + "], totalUsage: [" + publishCountInLastWeek + "]");
                } else {
                    details.add("Topic seems in use, not deleting: [" + topicName + "], totalUsage: [" + publishCountInLastWeek + "]");
                    LOGGER.warn("Topic seems in use, not deleting: [" + topicName + "], totalUsage: [" + publishCountInLastWeek + "]");
                    continue;
                }
            }
            topicsToDelete.add(snsResource);
            cloudwatchAlarmsToDelete.addAll(snsResource.getCloudwatchAlarms());
        }

        StringBuilder info = new StringBuilder()
                .append("The resources will be terminated regarding ").append(ticket).append("\n")
                .append("* Dry-Run: ").append(!apply).append("\n")
                .append("* Topics: ").append(topicsToDelete).append("\n")
                .append("* Cloudwatch alarms: ").append(cloudwatchAlarmsToDelete).append("\n");

        info.append("Details:\n");
        details.forEach(d -> info.append("-- ").append(d).append("\n"));
        LOGGER.info(info);

        return topicsToDelete;
    }

    @Override
    protected void apply(Set<SNSResource> resources, boolean apply) {
        if (!resources.isEmpty() && apply) {
            AmazonSNS snsClient = AWSClientProvider.getInstance(getConfiguration()).getAmazonSNS();
            AmazonCloudWatch cloudWatchClient = AWSClientProvider.getInstance(getConfiguration()).getAmazonCloudWatch();
            HashSet<String> cloudwatchAlarmsToDelete = new LinkedHashSet<>();

            List<String> topicsToDelete = resources
                    .stream()
                    .map(SNSResource::getResourceName)
                    .collect(Collectors.toList());
            resources
                    .stream()
                    .map(SNSResource::getCloudwatchAlarms)
                    .forEach(cloudwatchAlarmsToDelete::addAll);

            LOGGER.info("Terminating the resources...");

            topicsToDelete.forEach(snsClient::deleteTopic);

            if (!cloudwatchAlarmsToDelete.isEmpty()) {
                cloudWatchClient.deleteAlarms(new DeleteAlarmsRequest().withAlarmNames(cloudwatchAlarmsToDelete));
            }
        }
    }

    @Override
    protected void afterApply(Set<SNSResource> resources) {
        LOGGER.info("Succeed.");
    }

    private String getResourceFromArn(String arn) {
        return Arn.fromString(arn).getResource().getResource();
    }


    void setFetchResourceFactory(ResourceFetcherFactory<SNSResource> resourceFetcherFactory) {
        this.resourceFetcherFactory = resourceFetcherFactory;
    }

    private ResourceFetcherFactory<SNSResource> getFetchResourceFactory() {
        if (this.resourceFetcherFactory != null) {
            return this.resourceFetcherFactory;
        } else {
            return new ResourceFetcherFactory<>();
        }
    }
}
