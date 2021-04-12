package com.atlassian.awstool.terminate;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author Orhun Dalabasmaz
 * @version 10.03.2021
 */

public interface FetchResources<R extends AWSResource> {

    List<R> fetchResources(List<String> resources, List<String> details) throws Exception;

    void listResources(Consumer<List<String>> consumer) throws Exception;

    default Object getUsage(String region, String resource) {
        return null;
    }

    default void consume(Function<String, String> function) {
        String nextMarker = null;
        do {
            nextMarker = function.apply(nextMarker);
        }
        while (nextMarker != null);
    }
}
