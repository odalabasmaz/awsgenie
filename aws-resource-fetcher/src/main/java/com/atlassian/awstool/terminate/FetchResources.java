package com.atlassian.awstool.terminate;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author Orhun Dalabasmaz
 * @version 10.03.2021
 */

public interface FetchResources {
    List<? extends AWSResource> fetchResources(String region, List<String> resources, List<String> details) throws Exception;

    void listResources(String region, Consumer<List<? extends Object>> consumer) throws Exception;

    default <T> void consume(Function<String, String> function) {
        String nextMarker = null;
        do {
            nextMarker = function.apply(nextMarker);
        }
        while (nextMarker != null);
    }
}
