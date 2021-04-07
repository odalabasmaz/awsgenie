package com.atlassian.awstool.terminate;

import java.util.List;

/**
 * @author Orhun Dalabasmaz
 * @version 10.03.2021
 */

public interface FetchResources {
    List<? extends AWSResource> fetchResources(String region, String service, List<String> resources, List<String> details) throws Exception;
}
