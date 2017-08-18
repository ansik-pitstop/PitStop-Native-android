package com.pitstop.utils;

/**
 * Created by Karol Zdebel on 8/18/2017.
 */

public class ConnectionChecker {
    private NetworkHelper networkHelper;

    public ConnectionChecker(NetworkHelper networkHelper){
        this.networkHelper = networkHelper;
    }

    public boolean isConnected(){
        return networkHelper.isConnected();
    }
}
