package io.awsgenie.terminator;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.DeleteAlarmsRequest;
import io.awsgenie.fetcher.ResourceFetcher;
import io.awsgenie.fetcher.ResourceFetcherConfiguration;
import io.awsgenie.fetcher.ResourceFetcherFactory;
import io.awsgenie.fetcher.Service;
import io.awsgenie.fetcher.cloudwatch.CloudWatchResource;
import io.awsgenie.fetcher.credentials.AWSClientConfiguration;
import io.awsgenie.fetcher.credentials.AWSClientProvider;
import io.awsgenie.terminator.configuration.Configuration;
import io.awsgenie.terminator.interceptor.InterceptorRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Orhun Dalabasmaz
 * @version 10.03.2021
 */

public class CloudWatchResourceTerminator extends ResourceTerminatorWithProvider implements ResourceTerminator<CloudWatchResource> {
    private static final Logger LOGGER = LogManager.getLogger(CloudWatchResourceTerminator.class);

    private ResourceFetcherFactory<CloudWatchResource> resourceFetcherFactory;

    public CloudWatchResourceTerminator(AWSClientConfiguration configuration) {
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
        AmazonCloudWatch cloudWatchClient = AWSClientProvider.getInstance(getConfiguration()).getAmazonCloudWatch();

        ResourceFetcher<CloudWatchResource> fetcher = getFetchResourceFactory().getFetcher(service, new ResourceFetcherConfiguration(getConfiguration()));
        List<CloudWatchResource> cloudWatchResourceList = fetcher.fetchResources(region, resources, null);

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

        InterceptorRegistry.getBeforeTerminateInterceptors()
                .forEach(interceptor -> interceptor.intercept(service, cloudWatchResourceList, info.toString(), apply));

        if (apply) {
            LOGGER.info("Terminating the resources...");
            if (!cloudwatchAlarmsToDelete.isEmpty()) {
                cloudWatchClient.deleteAlarms(new DeleteAlarmsRequest().withAlarmNames(cloudwatchAlarmsToDelete));
            }
        }

        InterceptorRegistry.getAfterTerminateInterceptors()
                .forEach(interceptor -> interceptor.intercept(service, cloudWatchResourceList, info.toString(), apply));

        LOGGER.info("Succeed.");
    }

    void setFetchResourceFactory(ResourceFetcherFactory<CloudWatchResource> resourceFetcherFactory) {
        this.resourceFetcherFactory = resourceFetcherFactory;
    }

    private ResourceFetcherFactory<CloudWatchResource> getFetchResourceFactory() {
        if (this.resourceFetcherFactory != null) {
            return this.resourceFetcherFactory;
        } else {
            return new ResourceFetcherFactory<>();
        }
    }
}
