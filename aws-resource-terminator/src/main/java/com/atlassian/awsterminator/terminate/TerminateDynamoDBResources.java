package com.atlassian.awsterminator.terminate;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.DeleteAlarmsRequest;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.atlassian.awsterminator.interceptor.InterceptorRegistry;
import com.atlassian.awstool.terminate.FetchResourceFactory;
import com.atlassian.awstool.terminate.FetchResources;
import com.atlassian.awstool.terminate.FetcherConfiguration;
import com.atlassian.awstool.terminate.Service;
import com.atlassian.awstool.terminate.dynamodb.DynamoDBResource;
import credentials.AwsClientConfiguration;
import credentials.AwsClientProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Orhun Dalabasmaz
 * @version 10.03.2021
 */

public class TerminateDynamoDBResources extends TerminateResourcesWithProvider implements TerminateResources<DynamoDBResource> {
    private static final Logger LOGGER = LogManager.getLogger(TerminateDynamoDBResources.class);

    private FetchResourceFactory<DynamoDBResource> fetchResourceFactory;

    public TerminateDynamoDBResources(AwsClientConfiguration configuration) {
        super(configuration);
    }

    @Override
    public void terminateResource(String region, Service service, List<String> resources, String ticket, boolean apply) throws Exception {
        AmazonDynamoDB dynamoDBClient = AwsClientProvider.getInstance(getConfiguration()).getAmazonDynamoDB();

        AmazonCloudWatch cloudWatchClient = AwsClientProvider.getInstance(getConfiguration()).getAmazonCloudWatch();

        // Resources to be removed
        LinkedHashSet<String> tablesToDelete = new LinkedHashSet<>();
        LinkedHashSet<String> cloudwatchAlarmsToDelete = new LinkedHashSet<>();

        List<String> details = new LinkedList<>();

        FetchResources fetcher = getFetchResourceFactory().getFetcher(Service.DYNAMODB, new FetcherConfiguration(getConfiguration()));
        List<DynamoDBResource> dynamoDBResourceList = (List<DynamoDBResource>) fetcher.fetchResources(region, resources, details);

        for (DynamoDBResource dynamodbResource : dynamoDBResourceList) {
            Double totalUsage = (Double) fetcher.getUsage(region, dynamodbResource.getResourceName());
            if (totalUsage > 0) {
                details.add("DynamoDB table seems in use, not deleting: [" + dynamodbResource.getResourceName() + "], totalUsage: [" + totalUsage + "]");
                LOGGER.warn("DynamoDB table seems in use, not deleting: [" + dynamodbResource.getResourceName() + "], totalUsage: [" + totalUsage + "]");
                continue;
            }
            tablesToDelete.add(dynamodbResource.getResourceName());
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

    void setFetchResourceFactory(FetchResourceFactory<DynamoDBResource> fetchResourceFactory) {
        this.fetchResourceFactory = fetchResourceFactory;
    }

    private FetchResourceFactory<DynamoDBResource> getFetchResourceFactory() {
        if (this.fetchResourceFactory != null) {
            return this.fetchResourceFactory;
        } else {
            return new FetchResourceFactory<>();
        }
    }
}
