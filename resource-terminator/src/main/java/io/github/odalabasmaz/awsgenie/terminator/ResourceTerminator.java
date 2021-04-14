package io.github.odalabasmaz.awsgenie.terminator;

import io.github.odalabasmaz.awsgenie.fetcher.Resource;
import io.github.odalabasmaz.awsgenie.fetcher.Service;
import io.github.odalabasmaz.awsgenie.terminator.configuration.Configuration;

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
