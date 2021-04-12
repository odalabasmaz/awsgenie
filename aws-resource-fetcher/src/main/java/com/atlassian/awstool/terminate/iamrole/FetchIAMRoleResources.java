package com.atlassian.awstool.terminate.iamrole;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.identitymanagement.model.*;
import com.atlassian.awstool.terminate.FetchResources;
import com.atlassian.awstool.terminate.FetchResourcesWithProvider;
import com.atlassian.awstool.terminate.FetcherConfiguration;
import credentials.AwsClientProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author Orhun Dalabasmaz
 * @version 10.03.2021
 */

public class FetchIAMRoleResources extends FetchResourcesWithProvider implements FetchResources<IAMRoleResource> {
    private static final Logger LOGGER = LogManager.getLogger(FetchIAMRoleResources.class);

    public FetchIAMRoleResources(FetcherConfiguration configuration) {
        super(configuration);
    }


    @Override
    public Object getUsage(String region, String resource) {
        AmazonIdentityManagement iamClient = AwsClientProvider.getInstance(getConfiguration()).getAmazonIAM();

        Role role = iamClient.getRole(new GetRoleRequest().withRoleName(resource)).getRole();
        return role.getRoleLastUsed().getLastUsedDate();
    }

    @Override
    public void listResources(String region, Consumer<List<String>> consumer) {
        consume((nextMarker) -> {
            ListRolesResult listRolesResult = AwsClientProvider.getInstance(getConfiguration()).getAmazonIAM().listRoles(new ListRolesRequest().withMarker(nextMarker));
            List<String> roleList = listRolesResult.getRoles().stream().map(Role::getRoleName).collect(Collectors.toList());
            consumer.accept(roleList);
            return listRolesResult.getMarker();
        });
    }

    @Override
    public List<IAMRoleResource> fetchResources(String region, List<String> resources, List<String> details) {
        AmazonIdentityManagement iamClient = AwsClientProvider.getInstance(getConfiguration()).getAmazonIAM();

        List<IAMRoleResource> iamRoleResourceList = new ArrayList<>();

        for (String roleName : resources) {
            try {
                Role role = iamClient.getRole(new GetRoleRequest().withRoleName(roleName)).getRole();
                iamRoleResourceList.add(new IAMRoleResource().setResourceName(role.getRoleName()));
            } catch (NoSuchEntityException ex) {
                details.add("!!! IAM Role not exists: [" + roleName + "]");
                LOGGER.warn("!!! IAM Role not exists: [" + roleName + "]");
            }
        }
        return iamRoleResourceList;
    }
}
