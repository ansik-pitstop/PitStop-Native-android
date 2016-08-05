package com.pitstop.DataAccessLayer.DTOs;

/**
 * Created by Ben Wu on 2016-08-04.
 */
public class ObdScanner {

    private int carId;
    private String deviceName; // bt device name (IDD-212B 000000)
    private String scannerId; // name stored on device (212BM000000)
    private String datanum; // last datanum received from this device

    public ObdScanner() {
    }

    public ObdScanner(int carId, String deviceName, String scannerId) {
        this.carId = carId;
        this.deviceName = deviceName;
        this.scannerId = scannerId;
    }

    public ObdScanner(int carId, String scannerId) {
        this.carId = carId;
        this.scannerId = scannerId;
    }

    public int getCarId() {
        return carId;
    }

    public void setCarId(int carId) {
        this.carId = carId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getScannerId() {
        return scannerId;
    }

    public void setScannerId(String scannerId) {
        this.scannerId = scannerId;
    }

    public String getDatanum() {
        return datanum;
    }

    public void setDatanum(String datanum) {
        this.datanum = datanum;
    }
}
