package com.atlassian.awsterminator.terminate;

import com.atlassian.awsterminator.configuration.Configuration;
import com.atlassian.awstool.terminate.AWSResource;
import com.atlassian.awstool.terminate.Service;

import java.util.List;

/**
 * @author Orhun Dalabasmaz
 * @version 10.03.2021
 */

public interface TerminateResources<R extends AWSResource> {
    void terminateResource(Configuration conf, boolean apply) throws Exception;

    void terminateResource(String region, Service service, List<String> resources, String ticket, boolean apply) throws Exception;

    void terminateResource(String region, Service service, List<String> resources, String ticket, int lastUsage, boolean force, boolean apply) throws Exception;
}
