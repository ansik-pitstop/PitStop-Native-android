package com.pitstop.models;

/**
 * Created by Ben Wu on 2016-08-04.
 */
public class ObdScanner {

    private int carId;
    private String deviceName; // bt device name (IDD-212B 000000)
    private String scannerId; // name stored on device (212BM000000)
    private String datanum; // last datanum received from this device
    private Boolean status;   //active, not active, or not set(NULL)

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

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
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

    @Override
    public String toString(){
        return "device name: "+deviceName +", scanner id:"+scannerId+", car id: "+carId;
    }
}
