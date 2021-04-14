package com.atlassian.awsgenie.terminator.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;

/**
 * @author Celal Emre CICEK
 * @version 12.04.2021
 */

public class ConfigurationReader {
    private static final String DEFAULT_CONFIG_FILE_PATH = System.getProperty("user.home") + "/.awsterminator/config.json";

    public static Configuration getConfiguration(ParameterConfiguration parameterConfiguration) throws IOException {
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

    private static FileConfiguration getConfigurationFromFile(String path) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(new File(path), FileConfiguration.class);
    }

    static void mergeConfiguration(Configuration source, Configuration destination) {
        if (StringUtils.isNotEmpty(source.getRegion())) {
            destination.setRegion(source.getRegion());
        }

        if (StringUtils.isNotEmpty(source.getService())) {
            destination.setService(source.getService());
        }

        if (StringUtils.isNotEmpty(source.getResources())) {
            destination.setResources(source.getResources());
        }

        if (StringUtils.isNotEmpty(source.getDescription())) {
            destination.setDescription(source.getDescription());
        }

        if (StringUtils.isNotEmpty(source.getAssumeRoleArn())) {
            destination.setAssumeRoleArn(source.getAssumeRoleArn());
        }
    }
}
