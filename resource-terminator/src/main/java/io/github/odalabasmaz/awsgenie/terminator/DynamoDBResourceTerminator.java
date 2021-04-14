package io.github.odalabasmaz.awsgenie.terminator;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.DeleteAlarmsRequest;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import io.github.odalabasmaz.awsgenie.fetcher.ResourceFetcher;
import io.github.odalabasmaz.awsgenie.fetcher.ResourceFetcherConfiguration;
import io.github.odalabasmaz.awsgenie.fetcher.ResourceFetcherFactory;
import io.github.odalabasmaz.awsgenie.fetcher.Service;
import io.github.odalabasmaz.awsgenie.fetcher.credentials.AWSClientConfiguration;
import io.github.odalabasmaz.awsgenie.fetcher.credentials.AWSClientProvider;
import io.github.odalabasmaz.awsgenie.fetcher.dynamodb.DynamoDBResource;
import io.github.odalabasmaz.awsgenie.terminator.configuration.Configuration;
import io.github.odalabasmaz.awsgenie.terminator.interceptor.InterceptorRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Orhun Dalabasmaz
 * @version 10.03.2021
 */

public class DynamoDBResourceTerminator extends ResourceTerminatorWithProvider implements ResourceTerminator<DynamoDBResource> {
    private static final Logger LOGGER = LogManager.getLogger(DynamoDBResourceTerminator.class);

    private ResourceFetcherFactory<DynamoDBResource> resourceFetcherFactory;

    public DynamoDBResourceTerminator(AWSClientConfiguration configuration) {
        super(configuration);
    }

    @Override
    public void terminateResource(Configuration conf, boolean apply) throws Exception {
        terminateResource(conf.getRegion(), Service.fromValue(conf.getService()), conf.getResourcesAsList(), conf.getDescription(),
                conf.getLastUsage(), conf.isForce(), apply);
    }

    @Override
    //TODO: deprecate me :)
    public void terminateResource(String region, Service service, List<String> resources, String ticket, boolean apply) throws Exception {
        terminateResource(region, service, resources, ticket, 7, false, apply);
    }

    @Override
    public void terminateResource(String region, Service service, List<String> resources, String ticket, int lastUsage, boolean force, boolean apply) throws Exception {
        AmazonDynamoDB dynamoDBClient = AWSClientProvider.getInstance(getConfiguration()).getAmazonDynamoDB();

        AmazonCloudWatch cloudWatchClient = AWSClientProvider.getInstance(getConfiguration()).getAmazonCloudWatch();

        // Resources to be removed
        LinkedHashSet<String> tablesToDelete = new LinkedHashSet<>();
        LinkedHashSet<String> cloudwatchAlarmsToDelete = new LinkedHashSet<>();

        List<String> details = new LinkedList<>();

        ResourceFetcher<DynamoDBResource> fetcher = getFetchResourceFactory().getFetcher(service, new ResourceFetcherConfiguration(getConfiguration()));
        List<DynamoDBResource> dynamoDBResourceList = fetcher.fetchResources(region, resources, details);

        for (DynamoDBResource dynamodbResource : dynamoDBResourceList) {
            String tableName = dynamodbResource.getResourceName();
            Double totalUsage = (Double) fetcher.getUsage(region, tableName, lastUsage);
            if (totalUsage > 0) {
                if (force) {
                    details.add("DynamoDB table seems in use, but still deleting with force: [" + tableName + "], totalUsage: [" + totalUsage + "]");
                    LOGGER.warn("DynamoDB table seems in use, but still deleting with force: [" + tableName + "], totalUsage: [" + totalUsage + "]");
                } else {
                    details.add("DynamoDB table seems in use, not deleting: [" + tableName + "], totalUsage: [" + totalUsage + "]");
                    LOGGER.warn("DynamoDB table seems in use, not deleting: [" + tableName + "], totalUsage: [" + totalUsage + "]");
                    continue;
                }
            }
            tablesToDelete.add(tableName);
            cloudwatchAlarmsToDelete.addAll(dynamodbResource.getCloudwatchAlarmList());
        }

        StringBuilder info = new StringBuilder()
                .append("The resources will be terminated regarding ").append(ticket).append("\n")
                .append("* Dry-Run: ").append(!apply).append("\n")
                .append("* DynamoDB tables: ").append(tablesToDelete).append("\n")
                .append("* Cloudwatch alarms: ").append(cloudwatchAlarmsToDelete).append("\n");

        info.append("Details:\n");
        details.forEach(d -> info.append("-- ").append(d).append("\n"));
        LOGGER.info(info);

        InterceptorRegistry.getBeforeTerminateInterceptors()
                .forEach(interceptor -> interceptor.intercept(service, dynamoDBResourceList, info.toString(), apply));

        if (apply) {
            LOGGER.info("Terminating the resources...");

            tablesToDelete.forEach(dynamoDBClient::deleteTable);

            if (!cloudwatchAlarmsToDelete.isEmpty()) {
                cloudWatchClient.deleteAlarms(new DeleteAlarmsRequest().withAlarmNames(cloudwatchAlarmsToDelete));
            }
        }

        InterceptorRegistry.getAfterTerminateInterceptors()
                .forEach(interceptor -> interceptor.intercept(service, dynamoDBResourceList, info.toString(), apply));

        LOGGER.info("Succeed.");
    }

    void setFetchResourceFactory(ResourceFetcherFactory<DynamoDBResource> resourceFetcherFactory) {
        this.resourceFetcherFactory = resourceFetcherFactory;
    }

    private ResourceFetcherFactory<DynamoDBResource> getFetchResourceFactory() {
        if (this.resourceFetcherFactory != null) {
            return this.resourceFetcherFactory;
        } else {
            return new ResourceFetcherFactory<>();
        }
    }
}
