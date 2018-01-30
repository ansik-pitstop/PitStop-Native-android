package com.pitstop.bluetooth.dataPackages;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Ben Wu on 2016-09-02.
 */
public abstract class PidPackage {

    private String deviceId;
    private Map<String, String> pids; // key is pid type, value is value

    public PidPackage(String deviceId) {
        this.deviceId = deviceId;
        this.pids = new HashMap<>();
    }

    public PidPackage(String deviceId, Map<String,String> pids) {
        this.deviceId = deviceId;
        this.pids = pids;
    }

    public PidPackage(PidPackage pidPackage) {
        this.deviceId = pidPackage.deviceId;
        this.pids = pidPackage.pids;
    }

    public void addPid(String key, String value){
        this.pids.put(key,value);
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId){
        this.deviceId = deviceId;
    }

    public Map<String, String> getPids() {
        return pids;
    }

    public void setPids(HashMap<String, String> newpids){
        this.pids = newpids;
    }

    @Override
    public String toString() {
        return "PidPackage{" +
                "deviceId='" + deviceId + '\'' +
                ", pids=" + pids +
                '}';
    }

}
