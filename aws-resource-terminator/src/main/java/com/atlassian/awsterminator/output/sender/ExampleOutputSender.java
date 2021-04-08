package com.atlassian.awsterminator.output.sender;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Celal Emre CICEK
 * @version 8.04.2021
 */

public class ExampleOutputSender implements OutputSender {
    private static final Logger LOGGER = LogManager.getLogger(ExampleOutputSender.class);
    private static final String OUTPUT_SENDER_NAME = "example";

    @Override
    public String getName() {
        return OUTPUT_SENDER_NAME;
    }

    @Override
    public void send(Object output) {
        LOGGER.info(output);
    }
}
