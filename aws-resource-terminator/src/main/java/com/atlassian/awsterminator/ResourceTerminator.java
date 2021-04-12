package com.atlassian.awsterminator;

import com.atlassian.awsterminator.configuration.Configuration;
import com.atlassian.awsterminator.configuration.ConfigurationReader;
import com.atlassian.awsterminator.configuration.ParameterConfiguration;
import com.atlassian.awsterminator.terminate.TerminateResourceFactory;
import com.atlassian.awsterminator.terminate.TerminateResources;
import com.atlassian.awstool.terminate.Service;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;

/**
 * @author Orhun Dalabasmaz
 * @version 10.03.2021
 */

public class ResourceTerminator {
    private static final Logger LOGGER = LogManager.getLogger(ResourceTerminator.class);
    private static final String JIRA_SPACE_FOR_AUDIT = "hello";   // TODO: please fix me and make it configurable..

    public static void main(String[] args) throws Exception {
        ParameterConfiguration parameterConfiguration = new ParameterConfiguration();

        try {
            parameterConfiguration.fromSystemArgs(args);
        } catch (Exception e) {
            throw new IllegalArgumentException("Can not convert arguments " + Arrays.toString(args) + " to "
                    + ParameterConfiguration.class.getSimpleName() + " Reason " + e, e);
        }

        ResourceTerminator task = new ResourceTerminator();
        Configuration configuration = ConfigurationReader.getConfiguration(parameterConfiguration);
        configuration.validate();
        task.run(configuration, parameterConfiguration.isApply());
    }

    public void run(Configuration configuration) throws Exception {
        run(configuration, false);
    }

    public void run(Configuration configuration, boolean apply) throws Exception {
        String region = configuration.getRegion();
        String serviceName = configuration.getService();
        Service service = Service.fromValue(serviceName);
        List<String> resources = configuration.getResourcesAsList();
        String ticket = configuration.getTicket();
        String ticketUrl = "https://" + JIRA_SPACE_FOR_AUDIT + ".atlassian.net/browse/" + ticket;
        LOGGER.info("Terminating resources for service: {}, resources: {}, ticket: {}, dry-run: {}, region: {}",
                service, resources, ticket, !apply, region);

        TerminateResourceFactory factory = new TerminateResourceFactory();
        TerminateResources terminator = factory.getTerminator(service, configuration);
        terminator.terminateResource(region, service, resources, ticketUrl, apply);
    }
}
