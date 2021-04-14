package io.github.odalabasmaz.awsgenie.terminator.interceptor;

import io.github.odalabasmaz.awsgenie.fetcher.Resource;
import io.github.odalabasmaz.awsgenie.fetcher.Service;

import java.util.List;

/**
 * @author Celal Emre CICEK
 * @version 8.04.2021
 */

public interface TerminateInterceptor {
    void intercept(Service service, List<? extends Resource> resources, String info, boolean apply);
}
