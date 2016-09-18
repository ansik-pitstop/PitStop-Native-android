package com.pitstop.utils;

/**
 * Created by Ben Wu on 2016-09-16.
 */
public interface MessageListener {
    int STATUS_UPDATE = 0;
    int STATUS_SUCCESS = 1;
    int STATUS_FAILED = 2;

    void processMessage(int status, String message);
}
