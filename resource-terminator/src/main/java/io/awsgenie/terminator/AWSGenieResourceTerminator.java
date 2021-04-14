package io.awsgenie.terminator;

import io.awsgenie.fetcher.Service;
import io.awsgenie.terminator.configuration.Configuration;
import io.awsgenie.terminator.configuration.ConfigurationReader;
import io.awsgenie.terminator.configuration.ParameterConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;

/**
 * @author Orhun Dalabasmaz
 * @version 10.03.2021
 */

public class AWSGenieResourceTerminator {
    private static final Logger LOGGER = LogManager.getLogger(AWSGenieResourceTerminator.class);

    //TODO: extract main from here...
    public static void main(String[] args) throws Exception {
        ParameterConfiguration parameterConfiguration = new ParameterConfiguration();

        try {
            parameterConfiguration.fromSystemArgs(args);
        } catch (Exception e) {
            throw new IllegalArgumentException("Can not convert arguments " + Arrays.toString(args) + " to "
                    + ParameterConfiguration.class.getSimpleName() + " Reason " + e, e);
        }

        AWSGenieResourceTerminator task = new AWSGenieResourceTerminator();
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
        String description = configuration.getDescription();
        LOGGER.info("Terminating resources for service: {}, resources: {}, description: {}, dry-run: {}, region: {}",
                service, resources, description, !apply, region);

        ResourceTerminatorFactory factory = new ResourceTerminatorFactory();
        ResourceTerminator terminator = factory.getTerminator(service, configuration);
        terminator.terminateResource(configuration, apply);
    }
}
