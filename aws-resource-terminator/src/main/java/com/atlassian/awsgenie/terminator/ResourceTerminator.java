package com.atlassian.awsgenie.terminator;

import com.atlassian.awsgenie.fetcher.Resource;
import com.atlassian.awsgenie.fetcher.Service;
import com.atlassian.awsgenie.terminator.configuration.Configuration;

import java.util.List;

/**
 * @author Orhun Dalabasmaz
 * @version 10.03.2021
 */

public interface ResourceTerminator<R extends Resource> {
    void terminateResource(Configuration conf, boolean apply) throws Exception;

    void terminateResource(String region, Service service, List<String> resources, String ticket, boolean apply) throws Exception;

    void terminateResource(String region, Service service, List<String> resources, String ticket, int lastUsage, boolean force, boolean apply) throws Exception;
}
