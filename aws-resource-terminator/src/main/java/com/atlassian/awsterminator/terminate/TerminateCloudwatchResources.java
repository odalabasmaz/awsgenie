package com.atlassian.awsterminator.terminate;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.DeleteAlarmsRequest;
import com.atlassian.awsterminator.configuration.Configuration;
import com.atlassian.awsterminator.interceptor.InterceptorRegistry;
import com.atlassian.awstool.terminate.FetchResourceFactory;
import com.atlassian.awstool.terminate.FetchResources;
import com.atlassian.awstool.terminate.FetcherConfiguration;
import com.atlassian.awstool.terminate.Service;
import com.atlassian.awstool.terminate.cloudwatch.CloudwatchResource;
import credentials.AwsClientConfiguration;
import credentials.AwsClientProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Orhun Dalabasmaz
 * @version 10.03.2021
 */

public class TerminateCloudwatchResources extends TerminateResourcesWithProvider implements TerminateResources<CloudwatchResource> {
    private static final Logger LOGGER = LogManager.getLogger(TerminateCloudwatchResources.class);

    private FetchResourceFactory<CloudwatchResource> fetchResourceFactory;

    public TerminateCloudwatchResources(AwsClientConfiguration configuration) {
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
        AmazonCloudWatch cloudWatchClient = AwsClientProvider.getInstance(getConfiguration()).getAmazonCloudWatch();

        FetchResources<CloudwatchResource> fetcher = getFetchResourceFactory().getFetcher(service, new FetcherConfiguration(getConfiguration()));
        List<CloudwatchResource> cloudwatchResourceList = fetcher.fetchResources(region, resources, null);

        Set<String> cloudwatchAlarmsToDelete = new HashSet<>();
        Set<String> cloudwatchAlarmsNotToDelete = new HashSet<>(resources);
        for (CloudwatchResource cloudwatchResource : cloudwatchResourceList) {
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

        InterceptorRegistry.getBeforeTerminateInterceptors()
                .forEach(interceptor -> interceptor.intercept(service, cloudwatchResourceList, info.toString(), apply));

        if (apply) {
            LOGGER.info("Terminating the resources...");
            if (!cloudwatchAlarmsToDelete.isEmpty()) {
                cloudWatchClient.deleteAlarms(new DeleteAlarmsRequest().withAlarmNames(cloudwatchAlarmsToDelete));
            }
        }

        InterceptorRegistry.getAfterTerminateInterceptors()
                .forEach(interceptor -> interceptor.intercept(service, cloudwatchResourceList, info.toString(), apply));

        LOGGER.info("Succeed.");
    }

    void setFetchResourceFactory(FetchResourceFactory<CloudwatchResource> fetchResourceFactory) {
        this.fetchResourceFactory = fetchResourceFactory;
    }

    private FetchResourceFactory<CloudwatchResource> getFetchResourceFactory() {
        if (this.fetchResourceFactory != null) {
            return this.fetchResourceFactory;
        } else {
            return new FetchResourceFactory<>();
        }
    }
}
