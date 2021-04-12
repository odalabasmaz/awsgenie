package com.atlassian.awsterminator.configuration;

import com.atlassian.awsterminator.exception.ConfigurationValidationException;
import com.atlassian.awstool.terminate.FetcherConfiguration;
import com.fasterxml.jackson.annotation.JsonIgnore;
import credentials.AwsClientConfiguration;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Celal Emre CICEK
 * @version 6.04.2021
 */

public interface Configuration extends AwsClientConfiguration {
    String getRegion();

    Configuration setRegion(String region);

    String getService();

    Configuration setService(String service);

    String getResources();

    @JsonIgnore
    default List<String> getResourcesAsList() {
        return Arrays.stream(getResources().split(";"))
                .map(String::trim)
                .map(r -> r.replaceAll("\\+", " "))
                .distinct()
                .collect(Collectors.toList());
    }

    Configuration setResources(String resources);

    String getTicket();

    Configuration setTicket(String ticket);

    String getAssumeRoleArn();

    Configuration setAssumeRoleArn(String assumeRoleArn);

    Configuration cloneMe();

    default void validate() throws ConfigurationValidationException {
            if (StringUtils.isEmpty(getRegion())) {
                throw new ConfigurationValidationException("region is required.");
            }

            if (StringUtils.isEmpty(getService())) {
                throw new ConfigurationValidationException("service is required.");
            }

            if (StringUtils.isEmpty(getResources())) {
                throw new ConfigurationValidationException("resources is required.");
            }

            if (StringUtils.isEmpty(getTicket())) {
                throw new ConfigurationValidationException("ticket is required.");
            }
    }
}
