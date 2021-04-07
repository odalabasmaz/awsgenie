package com.atlassian.awscomparator.configuration;

import com.atlassian.awscomparator.exception.ConfigurationValidationException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Celal Emre CICEK
 * @version 6.04.2021
 */

public interface Configuration {
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
    }
}
