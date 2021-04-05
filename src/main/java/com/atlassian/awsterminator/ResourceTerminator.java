package com.atlassian.awsterminator;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClient;
import com.atlassian.awsterminator.terminate.TerminateResourceFactory;
import com.atlassian.awsterminator.terminate.TerminateResources;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author Orhun Dalabasmaz
 * @version 10.03.2021
 */

public class ResourceTerminator {
    private static final Logger LOGGER = LogManager.getLogger(ResourceTerminator.class);
    private static final String STS_SESSION_NAME_PREFIX = "aws_resource_terminator_";

    public static void main(String[] args) throws Exception {
        TerminateResourcesRequest request = new TerminateResourcesRequest();

        try {
            request.fromSystemArgs(args);
        } catch (Exception e) {
            throw new IllegalArgumentException("Can not convert arguments " + Arrays.toString(args) + " to "
                    + TerminateResourcesRequest.class.getSimpleName() + " Reason " + e, e);
        }

        ResourceTerminator task = new ResourceTerminator();
        task.run(request);
    }

    private void run(TerminateResourcesRequest request) throws Exception {
        String region = request.getRegion();
        String service = request.getService();
        List<String> resources = request.getResources();
        String ticket = request.getTicket();
        String ticketUrl = "https://hello.atlassian.net/browse/" + ticket;
        boolean apply = request.isApply();
        LOGGER.info("Terminating resources for service: {}, resources: {}, ticket: {}, dry-run: {}, region: {}",
                service, resources, ticket, !apply, region);

        TerminateResourceFactory factory = new TerminateResourceFactory();
        AWSCredentialsProvider credentialsProvider = getCredentialsProvider(request);
        TerminateResources terminator = factory.getTerminator(service, credentialsProvider);
        terminator.terminateResource(region, service, resources, ticketUrl, apply);
    }

    private AWSCredentialsProvider getCredentialsProvider(TerminateResourcesRequest request) {
        AWSSecurityTokenService stsClient = AWSSecurityTokenServiceClient.builder()
                .withRegion(request.getRegion())
                .build();

        if (StringUtils.isNotBlank(request.getAssumeRoleArn())) {
            STSAssumeRoleSessionCredentialsProvider credentialsProvider = new STSAssumeRoleSessionCredentialsProvider
                    .Builder(request.getAssumeRoleArn(), getSTSSessionName())
                    .withStsClient(stsClient)
                    .build();
            LOGGER.info("Using assumed role: " + request.getAssumeRoleArn());
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
}
