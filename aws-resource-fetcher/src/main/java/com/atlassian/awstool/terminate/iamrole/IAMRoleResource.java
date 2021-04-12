package com.atlassian.awstool.terminate.iamrole;

import com.atlassian.awstool.terminate.AWSResource;

import java.util.Date;
import java.util.LinkedHashSet;

public class IAMRoleResource extends AWSResource {
    private String resourceName;

    @Override
    public String getResourceName() {
        return resourceName;
    }

    public IAMRoleResource setResourceName(String resourceName) {
        this.resourceName = resourceName;
        return this;
    }
}
