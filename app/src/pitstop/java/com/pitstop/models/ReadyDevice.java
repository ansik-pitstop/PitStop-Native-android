package com.pitstop.models;

/**
 * Created by Karol Zdebel on 7/26/2017.
 */

public class ReadyDevice {

    private String vin;
    private String scannerId;
    private String scannerName;

    public ReadyDevice(String vin, String scannerId, String scannerName) {
        this.vin = vin;
        this.scannerId = scannerId;
        this.scannerName = scannerName;
    }

    public String getVin() {
        return vin;
    }

    public void setVin(String vin) {
        this.vin = vin;
    }

    public String getScannerId() {
        return scannerId;
    }

    public void setScannerId(String scannerId) {
        this.scannerId = scannerId;
    }

    public String getScannerName() {
        return scannerName;
    }

    public void setScannerName(String scannerName) {
        this.scannerName = scannerName;
    }
}
