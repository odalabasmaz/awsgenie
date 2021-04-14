package io.github.odalabasmaz.awsgenie.terminator;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.DeleteAlarmsRequest;
import com.amazonaws.services.cloudwatchevents.AmazonCloudWatchEvents;
import com.amazonaws.services.cloudwatchevents.model.DeleteRuleRequest;
import com.amazonaws.services.cloudwatchevents.model.RemoveTargetsRequest;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.DeleteEventSourceMappingRequest;
import com.amazonaws.services.lambda.model.DeleteFunctionRequest;
import com.amazonaws.services.sns.AmazonSNS;
import io.github.odalabasmaz.awsgenie.fetcher.Resource;
import io.github.odalabasmaz.awsgenie.fetcher.Service;
import io.github.odalabasmaz.awsgenie.fetcher.lambda.LambdaResource;
import io.github.odalabasmaz.awsgenie.terminator.interceptor.AfterTerminateInterceptor;
import io.github.odalabasmaz.awsgenie.terminator.interceptor.BeforeTerminateInterceptor;
import io.github.odalabasmaz.awsgenie.terminator.interceptor.InterceptorRegistry;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;


/**
 * @author Celal Emre CICEK
 * @version 9.04.2021
 */

public class LambdaResourceTerminatorTest extends TerminatorTest {
    private static final String TEST_REGION = "us-west-2";
    private static final String TEST_TICKET = "TEST-TICKET";
    private final Service service = Service.LAMBDA;

    private static final List<String> TEST_RESOURCES = new ArrayList<String>() {{
        add("lambda1");
        add("lambda2");
        add("lambda3");
    }};

    private static final List<Resource> TEST_FETCHED_RESOURCES = new ArrayList<Resource>() {{
        add(new LambdaResource()
                .setResourceName("lambda1")
                .setCloudwatchAlarms(new LinkedHashSet<String>() {{
                    add("lambda1 Lambda execution failed");
                }})
                .setCloudwatchRules(new LinkedHashSet<String>() {{
                    add("lambda1 CW rule");
                }})
                .setSnsTriggers(new LinkedHashSet<String>() {{
                    add("lambda1 SNS trigger");
                }})
                .setCloudwatchRuleTargets(new LinkedHashSet<String>() {{
                    add("lambda1 CW rule:target");
                }})
                .setEventSourceMappings(new LinkedHashSet<String>() {{
                    add("lambda1 event source mapping");
                }}));
        add(new LambdaResource()
                .setResourceName("lambda2")
                .setSnsTriggers(new LinkedHashSet<String>() {{
                    add("lambda2 SNS trigger");
                }}));
    }};

    @Mock
    private BeforeTerminateInterceptor beforeTerminateInterceptor;

    @Mock
    private AfterTerminateInterceptor afterTerminateInterceptor;

    private LambdaResourceTerminator lambdaResourceTerminator;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        InterceptorRegistry.getBeforeTerminateInterceptors().clear();
        InterceptorRegistry.getAfterTerminateInterceptors().clear();
        this.lambdaResourceTerminator = new LambdaResourceTerminator(TerminatorHelper.getRegion1Account1Configuration());
        this.lambdaResourceTerminator.setFetchResourceFactory(getFetchResourceFactory());

        doReturn(TEST_FETCHED_RESOURCES)
                .when(getFetchResources()).fetchResources(eq(TEST_REGION), eq(TEST_RESOURCES), org.mockito.Mockito.any(List.class));
    }

    @Test
    public void terminateResourcesWithApply() throws Exception {
        lambdaResourceTerminator.terminateResource(TEST_REGION, service, TEST_RESOURCES, TEST_TICKET, true);

        AmazonSNS snsClient = getAmazonSNS();
        AWSLambda lambdaClient = getAmazonLambda();
        AmazonCloudWatchEvents cloudWatchEventsClient = getCloudWatchEvents();
        AmazonCloudWatch cloudWatchClient = getCloudWatchClient();
        verify(snsClient).unsubscribe("lambda1 SNS trigger");
        verify(snsClient).unsubscribe("lambda2 SNS trigger");
        verifyNoMoreInteractions(snsClient);
        verify(lambdaClient)
                .deleteEventSourceMapping(new DeleteEventSourceMappingRequest().withUUID("lambda1 event source mapping"));
        verify(lambdaClient).deleteFunction(new DeleteFunctionRequest().withFunctionName("lambda1"));
        verify(lambdaClient).deleteFunction(new DeleteFunctionRequest().withFunctionName("lambda2"));
        verifyNoMoreInteractions(lambdaClient);
        verify(cloudWatchEventsClient)
                .removeTargets(new RemoveTargetsRequest().withRule("lambda1 CW rule").withIds("target"));
        verify(cloudWatchEventsClient).deleteRule(new DeleteRuleRequest().withName("lambda1 CW rule"));
        verifyNoMoreInteractions(cloudWatchEventsClient);
        verify(cloudWatchClient).deleteAlarms(new DeleteAlarmsRequest().withAlarmNames("lambda1 Lambda execution failed"));
        verifyNoMoreInteractions(cloudWatchClient);
    }

    @Test
    public void terminateResourcesWithoutApply() throws Exception {
        lambdaResourceTerminator.terminateResource(TEST_REGION, service, TEST_RESOURCES, TEST_TICKET, false);
        AmazonSNS snsClient = getAmazonSNS();
        AWSLambda lambdaClient = getAmazonLambda();
        AmazonCloudWatchEvents cloudWatchEventsClient = getCloudWatchEvents();
        AmazonCloudWatch cloudWatchClient = getCloudWatchClient();
        verifyZeroInteractions(lambdaClient);
        verifyZeroInteractions(cloudWatchEventsClient);
        verifyZeroInteractions(snsClient);
        verifyZeroInteractions(cloudWatchClient);
    }

    @Test
    public void interceptorsAreCalled() throws Exception {
        InterceptorRegistry.addInterceptor(beforeTerminateInterceptor);
        InterceptorRegistry.addInterceptor(afterTerminateInterceptor);
        lambdaResourceTerminator.terminateResource(TEST_REGION, service, TEST_RESOURCES, TEST_TICKET, false);
        verify(beforeTerminateInterceptor).intercept(eq(service), eq(TEST_FETCHED_RESOURCES), any(String.class), eq(false));
        verify(afterTerminateInterceptor).intercept(eq(service), eq(TEST_FETCHED_RESOURCES), any(String.class), eq(false));
    }
}