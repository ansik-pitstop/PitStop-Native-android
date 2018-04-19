package com.pitstop.bluetooth.dataPackages;

/**
 * Created by Karol Zdebel on 1/30/2018.
 */

public class ELM327PidPackage extends PidPackage {
    public ELM327PidPackage(String deviceId, long timestamp) {
        super(deviceId,timestamp);
    }

    public ELM327PidPackage(ELM327PidPackage pidPackage) {
        super(pidPackage);
    }
}
