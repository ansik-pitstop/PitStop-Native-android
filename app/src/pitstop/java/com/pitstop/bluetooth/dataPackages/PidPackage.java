package com.pitstop.bluetooth.dataPackages;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Ben Wu on 2016-09-02.
 */
public abstract class PidPackage {

    private String deviceId;
    private Map<String, String> pids; // key is pid type, value is value
    private long timestamp;

    public PidPackage(String deviceId, long timestamp) {
        this.deviceId = deviceId;
        this.pids = new HashMap<>();
        this.timestamp = timestamp;
    }

    public PidPackage(String deviceId, long timestamp, Map<String,String> pids) {
        this.deviceId = deviceId;
        this.timestamp = timestamp;
        this.pids = pids;
    }

    public PidPackage(PidPackage pidPackage) {
        this.deviceId = pidPackage.deviceId;
        this.timestamp = pidPackage.timestamp;
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

    public void setPids(Map<String, String> newpids){
        this.pids = newpids;
    }

    public long getTimestamp(){
        return timestamp;
    }

    @Override
    public String toString() {
        return "PidPackage{" +
                "deviceId='" + deviceId + '\'' +
                ", pids=" + pids +
                '}';
    }

}
