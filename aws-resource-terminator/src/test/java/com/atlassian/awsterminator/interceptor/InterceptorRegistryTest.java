package com.atlassian.awsterminator.interceptor;

import com.atlassian.awstool.terminate.AWSResource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;


/**
 * @author Celal Emre CICEK
 * @version 9.04.2021
 */

@RunWith(MockitoJUnitRunner.class)
public class InterceptorRegistryTest {
    @Before
    public void setUp() throws Exception {
        InterceptorRegistry.getBeforeTerminateInterceptors().clear();
        InterceptorRegistry.getAfterTerminateInterceptors().clear();
    }

    @Test
    public void addInterceptor() throws Exception {
        InterceptorRegistry.addInterceptor(new TestBeforeInterceptor());
        InterceptorRegistry.addInterceptor(new TestAfterInterceptor());
        InterceptorRegistry.addInterceptor(new TestAfterInterceptor());

        assertThat(InterceptorRegistry.getBeforeTerminateInterceptors().size(), is(equalTo(1)));
        assertThat(InterceptorRegistry.getAfterTerminateInterceptors().size(), is(equalTo(2)));
    }

    @Test(expected = RuntimeException.class)
    public void addIncerceptorWithInvalidType() throws Exception {
        InterceptorRegistry.addInterceptor(new TestUnsupportedInterceptor());
    }

    class TestBeforeInterceptor implements BeforeTerminateInterceptor {
        @Override
        public void intercept(String service, List<? extends AWSResource> resources, String info, boolean apply) {

        }
    }

    class TestAfterInterceptor implements AfterTerminateInterceptor {
        @Override
        public void intercept(String service, List<? extends AWSResource> resources, String info, boolean apply) {

        }
    }

    class TestUnsupportedInterceptor implements TerminateInterceptor {
        @Override
        public void intercept(String service, List<? extends AWSResource> resources, String info, boolean apply) {

        }
    }
}