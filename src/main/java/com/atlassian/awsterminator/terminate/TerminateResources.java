package com.atlassian.awsterminator.terminate;

import java.util.List;

/**
 * @author Orhun Dalabasmaz
 * @version 10.03.2021
 */

public interface TerminateResources {
    void terminateResource(String region, String service, List<String> resources, String ticket, boolean apply) throws Exception;
}
