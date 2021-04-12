package com.atlassian.awsterminator.terminate;

import com.atlassian.awstool.terminate.AWSResource;
import com.atlassian.awstool.terminate.Service;

import java.util.List;

/**
 * @author Orhun Dalabasmaz
 * @version 10.03.2021
 */

public interface TerminateResources<R extends AWSResource> {
    void terminateResource(String region, Service service, List<String> resources, String ticket, boolean apply) throws Exception;
}
