package com.atlassian.awsterminator.output.registry;

import com.atlassian.awsterminator.output.sender.OutputSender;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reflections.Reflections;

import java.util.*;

/**
 * @author Celal Emre CICEK
 * @version 8.04.2021
 */

public class OutputSenderRegistry {
    private static final Logger LOGGER = LogManager.getLogger(OutputSenderRegistry.class);

    private static final String BASE_PACKAGE = "com.atlassian.awsterminator";
    private static final Map<String, OutputSender> OUTPUT_SENDERS = new LinkedHashMap<>();

    public static void registerOutputSenders() {
        Reflections reflections = new Reflections(BASE_PACKAGE);
        Set<Class<? extends OutputSender>> outputSenderClasses = reflections.getSubTypesOf(OutputSender.class);

        outputSenderClasses.forEach(clazz -> {
            try {
                OutputSender outputSender = clazz.newInstance();
                OUTPUT_SENDERS.put(outputSender.getName(), outputSender);
            } catch (IllegalAccessException|InstantiationException e) {
                LOGGER.error("Could not initialize output sender [" + clazz.getSimpleName() + "]: " + e, e);
            }
        });
    }

    public static void sendOutputToAll(Object output) {
        OUTPUT_SENDERS.forEach((name, clazz) -> clazz.send(output));
    }

    public static void sendOutput(String outputSenderName, Object output) {
        OutputSender outputSender = OUTPUT_SENDERS.get(outputSenderName);

        if (outputSender == null) {
            throw new RuntimeException("No output sender found with name [" + outputSenderName + "]");
        }

        outputSender.send(output);
    }
}
