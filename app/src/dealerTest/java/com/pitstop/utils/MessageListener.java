package com.pitstop.utils;

import com.pitstop.bluetooth.BluetoothAutoConnectService.State;

/**
 * Created by Ben Wu on 2016-09-16.
 */
public interface MessageListener {
    int STATUS_UPDATE = 0;
    int STATUS_SUCCESS = 1;
    int STATUS_FAILED = 2;

    void processMessage(int status, State state, String message);
}
