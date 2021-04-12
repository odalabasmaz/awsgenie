package com.atlassian.comparator;

import com.atlassian.awstool.terminate.AWSResource;
import com.atlassian.awstool.terminate.FetchResources;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ResourceProducerTest<K extends AWSResource> {

    @Mock
    FetchResources<K> fetchResources;

    @Mock
    ResourceQueue<String> queue;

    ResourceProducer<K> resourceProducer;

    @Before
    public void setup() {
        resourceProducer = new ResourceProducer<>(fetchResources, queue);
    }

    @Test
    public void checkResourceIsExist() throws Exception {
        List<String> resourceList = Arrays.asList("resource1", "resource2", "resource3");
        doAnswer(invocationOnMock -> {
            Consumer<List<String>> consumer = invocationOnMock.getArgument(0);
            consumer.accept(resourceList);
            return "";
        }).when(fetchResources).listResources(any());
        resourceProducer._run(fetchResources);
        verify(queue).addAll(resourceList);
    }


}