package com.atlassian.awstool.terminate.iamrole;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.identitymanagement.model.GetRoleRequest;
import com.amazonaws.services.identitymanagement.model.NoSuchEntityException;
import com.amazonaws.services.identitymanagement.model.Role;
import com.atlassian.awstool.terminate.AWSResource;
import com.atlassian.awstool.terminate.FetchResources;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Orhun Dalabasmaz
 * @version 10.03.2021
 */

public class FetchIAMRoleResources implements FetchResources {
    private static final Logger LOGGER = LogManager.getLogger(FetchIAMRoleResources.class);

    private final AWSCredentialsProvider credentialsProvider;

    public FetchIAMRoleResources(AWSCredentialsProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
    }

    @Override
    public List<? extends AWSResource> fetchResources(String region, String service, List<String> resources, List<String> details) {
        AmazonIdentityManagement iamClient = AmazonIdentityManagementClient
                .builder()
                .withRegion(region)
                .withCredentials(credentialsProvider)
                .build();

        List<IAMRoleResource> iamRoleResourceList = new ArrayList<>();

        for (String roleName : resources) {
            try {
                Role role = iamClient.getRole(new GetRoleRequest().withRoleName(roleName)).getRole();

                iamRoleResourceList.add(new IAMRoleResource().setResourceName(roleName).setLastUsedDate(role.getRoleLastUsed().getLastUsedDate()));
            } catch (NoSuchEntityException ex) {
                details.add("!!! IAM Role not exists: [" + roleName + "]");
                LOGGER.warn("!!! IAM Role not exists: [" + roleName + "]");
            }
        }
        return iamRoleResourceList;
    }
}
