package com.atlassian.awstool.terminate.iamPolicy;

import com.atlassian.awstool.terminate.AWSResource;

import java.util.Date;
import java.util.LinkedHashSet;

public class IAMPolicyResource extends AWSResource {
    private String resourceName;
    private Date lastUsedDate;

    @Override
    public String getResourceName() {
        return resourceName;
    }

    public IAMPolicyResource setResourceName(String resourceName) {
        this.resourceName = resourceName;
        return this;
    }

    public Date getLastUsedDate() {
        return lastUsedDate;
    }

    public IAMPolicyResource setLastUsedDate(Date lastUsedDate) {
        this.lastUsedDate = lastUsedDate;
        return this;
    }
}
