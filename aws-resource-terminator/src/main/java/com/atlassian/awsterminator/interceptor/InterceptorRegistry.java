package com.atlassian.awsterminator.interceptor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Celal Emre CICEK
 * @version 8.04.2021
 */

public class InterceptorRegistry {
    private static final List<BeforeTerminateInterceptor> BEFORE_TERMINATE_INTERCEPTORS = new ArrayList<>();
    private static final List<AfterTerminateInterceptor> AFTER_TERMINATE_INTERCEPTORS = new ArrayList<>();

    public static void addInterceptor(TerminateInterceptor interceptor) {
        if (interceptor instanceof BeforeTerminateInterceptor) {
            BEFORE_TERMINATE_INTERCEPTORS.add((BeforeTerminateInterceptor) interceptor);
        } else if (interceptor instanceof AfterTerminateInterceptor) {
            AFTER_TERMINATE_INTERCEPTORS.add((AfterTerminateInterceptor) interceptor);
        } else {
            throw new RuntimeException("Class " + interceptor.getClass().getSimpleName() + " is not a valid interceptor.");
        }
    }

    public static List<BeforeTerminateInterceptor> getBeforeTerminateInterceptors() {
        return BEFORE_TERMINATE_INTERCEPTORS;
    }

    public static List<AfterTerminateInterceptor> getAfterTerminateInterceptors() {
        return AFTER_TERMINATE_INTERCEPTORS;
    }
}
