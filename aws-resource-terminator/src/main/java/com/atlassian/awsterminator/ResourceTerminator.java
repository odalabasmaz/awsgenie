package com.atlassian.awsterminator;

import com.atlassian.awsterminator.configuration.Configuration;
import com.atlassian.awsterminator.configuration.FileConfiguration;
import com.atlassian.awsterminator.configuration.ParameterConfiguration;
import com.atlassian.awsterminator.terminate.TerminateResourceFactory;
import com.atlassian.awsterminator.terminate.TerminateResources;
import com.atlassian.awstool.terminate.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author Orhun Dalabasmaz
 * @version 10.03.2021
 */

public class ResourceTerminator {
    private static final Logger LOGGER = LogManager.getLogger(ResourceTerminator.class);
    private static final String DEFAULT_CONFIG_FILE_PATH = System.getProperty("user.home") + "/.awsterminator/config.json";
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
        Configuration configuration = task.getConfiguration(parameterConfiguration);
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

    private Configuration getConfiguration(ParameterConfiguration parameterConfiguration) throws IOException {
        File defaultConfigurationFile = new File(DEFAULT_CONFIG_FILE_PATH);
        FileConfiguration defaultConfiguration = null;

        if (defaultConfigurationFile.exists()) {
            defaultConfiguration = getConfigurationFromFile(DEFAULT_CONFIG_FILE_PATH);
        }

        if (StringUtils.isNotEmpty(parameterConfiguration.getConfigurationFile())) {
            FileConfiguration fileConfigurationFromParameter = getConfigurationFromFile(parameterConfiguration.getConfigurationFile());

            if (defaultConfiguration != null) {
                mergeConfiguration(fileConfigurationFromParameter, defaultConfiguration);
            } else {
                defaultConfiguration = fileConfigurationFromParameter.cloneMe();
            }
        }

        if (defaultConfiguration != null) {
            mergeConfiguration(parameterConfiguration, defaultConfiguration);
            return defaultConfiguration;
        }

        return parameterConfiguration;
    }

    private FileConfiguration getConfigurationFromFile(String path) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(new File(path), FileConfiguration.class);
    }

    private static void mergeConfiguration(Configuration source, Configuration destination) {
        if (StringUtils.isNotEmpty(source.getRegion())) {
            destination.setRegion(source.getRegion());
        }

        if (StringUtils.isNotEmpty(source.getService())) {
            destination.setService(source.getService());
        }

        if (StringUtils.isNotEmpty(source.getResources())) {
            destination.setResources(source.getResources());
        }

        if (StringUtils.isNotEmpty(source.getTicket())) {
            destination.setTicket(source.getTicket());
        }

        if (StringUtils.isNotEmpty(source.getAssumeRoleArn())) {
            destination.setAssumeRoleArn(source.getAssumeRoleArn());
        }
    }
}
