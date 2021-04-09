package com.atlassian.awsterminator.interceptor;

import com.atlassian.awstool.terminate.AWSResource;

import java.util.List;

/**
 * @author Celal Emre CICEK
 * @version 8.04.2021
 */

public interface TerminateInterceptor {
    void intercept(String service, List<? extends AWSResource> resources, String info, boolean apply);
}
