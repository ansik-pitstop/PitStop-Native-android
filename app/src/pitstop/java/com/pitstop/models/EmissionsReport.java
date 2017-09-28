package com.pitstop.models;

import java.util.Date;

/**
 * Created by Karol Zdebel on 9/28/2017.
 */

//{"response":{"id":116,"content":{"id":"2141"
// ,"data":{
// "Misfire":"N/A"
// ,"RawData":"0F0C14FF"
// ,"Ignition":"Compression"
// ,"Reserved1":"Incomplete"
// ,"Reserved2":"Incomplete"
// ,"Components":"Complete"
// ,"Fuel System":"N/A"
// ,"NMHC Catalyst":"N/A"
// ,"Boost Pressure":"N/A"
// ,"EGR/VVT System":"N/A"
// ,"Exhaust Sensor":"N/A"
// ,"NOx/SCR Monitor":"N/A"
// ,"PM Filter Monitoring":"N/A"}
// ,"pass":false}
// ,"meta":null
// ,"carId":5111
// ,"isInternal":false
// ,"reportType":"emission"
// ,"createdAt":"2017-09-28T16:02:36.185Z"
// ,"updatedAt":null}
// ,"meta":{}}

public class EmissionsReport {

    private int id;
    private String misfire;
    private String ignition;
    private String components;
    private String fuelSystem;
    private String NMHCCatalyst;
    private String boostPressure;
    private String EGRVTTSystem;
    private String exhaustSensor;
    private String NOxSCRMonitor;
    private String PMFilterMonitoring;
    private Date createdAt;
    private boolean pass;

    public EmissionsReport(int id, String misfire, String ignition, String components
            , String fuelSystem, String NMHCCatalyst, String boostPressure
            , String EGRVTTSystem, String exhaustSensor, String NOxSCRMonitor
            , String PMFilterMonitoring, Date createdAt, boolean pass) {

        this.id = id;
        this.misfire = misfire;
        this.ignition = ignition;
        this.components = components;
        this.fuelSystem = fuelSystem;
        this.NMHCCatalyst = NMHCCatalyst;
        this.boostPressure = boostPressure;
        this.EGRVTTSystem = EGRVTTSystem;
        this.exhaustSensor = exhaustSensor;
        this.NOxSCRMonitor = NOxSCRMonitor;
        this.PMFilterMonitoring = PMFilterMonitoring;
        this.createdAt = createdAt;
        this.pass = pass;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMisfire() {
        return misfire;
    }

    public void setMisfire(String misfire) {
        this.misfire = misfire;
    }

    public String getIgnition() {
        return ignition;
    }

    public void setIgnition(String ignition) {
        this.ignition = ignition;
    }

    public String getComponents() {
        return components;
    }

    public void setComponents(String components) {
        this.components = components;
    }

    public String getFuelSystem() {
        return fuelSystem;
    }

    public void setFuelSystem(String fuelSystem) {
        this.fuelSystem = fuelSystem;
    }

    public String getNMHCCatalyst() {
        return NMHCCatalyst;
    }

    public void setNMHCCatalyst(String NMHCCatalyst) {
        this.NMHCCatalyst = NMHCCatalyst;
    }

    public String getBoostPressure() {
        return boostPressure;
    }

    public void setBoostPressure(String boostPressure) {
        this.boostPressure = boostPressure;
    }

    public String getEGRVTTSystem() {
        return EGRVTTSystem;
    }

    public void setEGRVTTSystem(String EGRVTTSystem) {
        this.EGRVTTSystem = EGRVTTSystem;
    }

    public String getExhaustSensor() {
        return exhaustSensor;
    }

    public void setExhaustSensor(String exhaustSensor) {
        this.exhaustSensor = exhaustSensor;
    }

    public String getNOxSCRMonitor() {
        return NOxSCRMonitor;
    }

    public void setNOxSCRMonitor(String NOxSCRMonitor) {
        this.NOxSCRMonitor = NOxSCRMonitor;
    }

    public String getPMFilterMonitoring() {
        return PMFilterMonitoring;
    }

    public void setPMFilterMonitoring(String PMFilterMonitoring) {
        this.PMFilterMonitoring = PMFilterMonitoring;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isPass() {
        return pass;
    }

    public void setPass(boolean pass) {
        this.pass = pass;
    }
}
