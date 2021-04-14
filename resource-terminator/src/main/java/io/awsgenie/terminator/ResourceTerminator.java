package io.awsgenie.terminator;

import io.awsgenie.fetcher.Resource;
import io.awsgenie.fetcher.Service;
import io.awsgenie.terminator.configuration.Configuration;

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
