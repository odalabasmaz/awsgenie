package io.github.odalabasmaz.awsgenie.terminator.configuration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.odalabasmaz.awsgenie.fetcher.credentials.AWSClientConfiguration;
import io.github.odalabasmaz.awsgenie.terminator.exception.ConfigurationValidationException;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Interface for supported configuration types.
 * {@link ParameterConfiguration}
 * {@link FileConfiguration}
 */
public interface Configuration extends AWSClientConfiguration {
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

    int getLastUsage();

    Configuration setLastUsage(int lastUsage);

    String getDescription();

    Configuration setDescription(String ticket);

    String getAssumeRoleArn();

    Configuration setAssumeRoleArn(String assumeRoleArn);

    boolean isForce();

    Configuration setForce(boolean force);

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

        if (StringUtils.isEmpty(getDescription())) {
            throw new ConfigurationValidationException("ticket is required.");
        }
    }
}
