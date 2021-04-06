package com.atlassian.awsterminator;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClient;
import com.atlassian.awsterminator.configuration.Configuration;
import com.atlassian.awsterminator.configuration.FileConfiguration;
import com.atlassian.awsterminator.configuration.ParameterConfiguration;
import com.atlassian.awsterminator.terminate.TerminateResourceFactory;
import com.atlassian.awsterminator.terminate.TerminateResources;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author Orhun Dalabasmaz
 * @version 10.03.2021
 */

public class ResourceTerminator {
    private static final Logger LOGGER = LogManager.getLogger(ResourceTerminator.class);
    private static final String STS_SESSION_NAME_PREFIX = "aws_resource_terminator_";
    private static final String DEFAULT_CONFIG_FILE_PATH = System.getProperty("user.home") + "/.awsterminator/config.json";

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

    public void run(Configuration configuration, boolean apply) throws Exception {
        String region = configuration.getRegion();
        String service = configuration.getService();
        List<String> resources = configuration.getResourcesAsList();
        String ticket = configuration.getTicket();
        String ticketUrl = "https://hello.atlassian.net/browse/" + ticket;
        LOGGER.info("Terminating resources for service: {}, resources: {}, ticket: {}, dry-run: {}, region: {}",
                service, resources, ticket, !apply, region);

        TerminateResourceFactory factory = new TerminateResourceFactory();
        AWSCredentialsProvider credentialsProvider = getCredentialsProvider(configuration);
        TerminateResources terminator = factory.getTerminator(service, credentialsProvider);
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

    private AWSCredentialsProvider getCredentialsProvider(Configuration configuration) {
        AWSSecurityTokenService stsClient = AWSSecurityTokenServiceClient.builder()
                .withRegion(configuration.getRegion())
                .build();

        if (StringUtils.isNotBlank(configuration.getAssumeRoleArn())) {
            STSAssumeRoleSessionCredentialsProvider credentialsProvider = new STSAssumeRoleSessionCredentialsProvider
                    .Builder(configuration.getAssumeRoleArn(), getSTSSessionName())
                    .withStsClient(stsClient)
                    .build();
            LOGGER.info("Using assumed role: " + configuration.getAssumeRoleArn());
            return credentialsProvider;
        }

        return null;
    }

    private static String getSTSSessionName() {
        SimpleDateFormat dateFormatter = new SimpleDateFormat("HH_mm", Locale.ENGLISH);
        dateFormatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        String datePrefix = dateFormatter.format(new Date());

        return STS_SESSION_NAME_PREFIX + datePrefix;
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