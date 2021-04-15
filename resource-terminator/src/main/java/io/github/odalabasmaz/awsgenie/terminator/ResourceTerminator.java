package io.github.odalabasmaz.awsgenie.terminator;

import io.github.odalabasmaz.awsgenie.fetcher.Resource;
import io.github.odalabasmaz.awsgenie.fetcher.Service;
import io.github.odalabasmaz.awsgenie.fetcher.credentials.AWSClientConfiguration;
import io.github.odalabasmaz.awsgenie.terminator.configuration.Configuration;
import io.github.odalabasmaz.awsgenie.terminator.interceptor.InterceptorRegistry;

import java.util.List;
import java.util.Set;

/**
 * Checks resources in AWS account, and if you allow deletes them.
 *
 * @param <R> Resource type. Check child classes of {@link io.github.odalabasmaz.awsgenie.fetcher.Resource} class
 *            to see supported resource types.
 */
public abstract class ResourceTerminator<R extends Resource> {
    private final AWSClientConfiguration configuration;

    public ResourceTerminator(AWSClientConfiguration configuration) {
        this.configuration = configuration;
    }

    public AWSClientConfiguration getConfiguration() {
        return configuration;
    }

    public void terminateResource(Configuration conf, boolean apply) throws Exception {
        Service service = Service.fromValue(conf.getService());
        Set<R> resourcesToDelete = beforeApply(conf, apply);
        InterceptorRegistry.getBeforeTerminateInterceptors()
                .forEach(interceptor -> interceptor.intercept(service, resourcesToDelete, apply));
        apply(resourcesToDelete, apply);
        InterceptorRegistry.getAfterTerminateInterceptors()
                .forEach(interceptor -> interceptor.intercept(service, resourcesToDelete, apply));
        afterApply(resourcesToDelete);
    }

    public void terminateResource(String region, Service service, List<String> resources, String ticket, boolean apply) throws Exception {
        Set<R> resourcesToDelete = beforeApply(region, service, resources, ticket, apply);
        InterceptorRegistry.getBeforeTerminateInterceptors()
                .forEach(interceptor -> interceptor.intercept(service, resourcesToDelete, apply));
        apply(resourcesToDelete, apply);
        InterceptorRegistry.getAfterTerminateInterceptors()
                .forEach(interceptor -> interceptor.intercept(service, resourcesToDelete, apply));
        afterApply(resourcesToDelete);
    }

    public final void terminateResource(String region, Service service, List<String> resources, String ticket, int lastUsage,
                                        boolean force, boolean apply) throws Exception {
        Set<R> resourcesToDelete = beforeApply(region, service, resources, ticket, lastUsage, force, apply);
        InterceptorRegistry.getBeforeTerminateInterceptors()
                .forEach(interceptor -> interceptor.intercept(service, resourcesToDelete, apply));
        apply(resourcesToDelete, apply);
        InterceptorRegistry.getAfterTerminateInterceptors()
                .forEach(interceptor -> interceptor.intercept(service, resourcesToDelete, apply));
        afterApply(resourcesToDelete);
    }

    protected abstract Set<R> beforeApply(Configuration conf, boolean apply) throws Exception;

    protected abstract Set<R> beforeApply(String region, Service service, List<String> resources, String ticket, boolean apply) throws Exception;

    protected abstract Set<R> beforeApply(String region, Service service, List<String> resources, String ticket, int lastUsage, boolean force, boolean apply) throws Exception;

    protected abstract void apply(Set<R> resources, boolean apply);

    protected abstract void afterApply(Set<R> resources);
}
