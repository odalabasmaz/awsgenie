package com.atlassian.awsterminator.output.sender;

/**
 * @author Celal Emre CICEK
 * @version 8.04.2021
 */

public interface OutputSender {
    String getName();
    void send(Object output);
}
