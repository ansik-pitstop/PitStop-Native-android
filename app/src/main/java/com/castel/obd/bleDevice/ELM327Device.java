package com.castel.obd.bleDevice;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import com.pitstop.bluetooth.BluetoothDeviceManager;

import java.util.UUID;

/**
 * Created by ishan on 2017-12-08.
 */

public class ELM327Device implements AbstractDevice {

    @Override
    public UUID getServiceUuid() {
        return null;
    }

    @Override
    public UUID getReadChar() {
        return null;
    }

    @Override
    public UUID getWriteChar() {
        return null;
    }

    @Override
    public BluetoothDeviceManager.CommType commType() {
        return null;
    }

    @Override
    public byte[] getBytes(String payload) {
        return new byte[0];
    }

    @Override
    public void parseData(byte[] data) {

    }

    @Override
    public void setManagerState(int state) {

    }

    @Override
    public void requestData() {

    }

    @Override
    public String getDeviceName() {
        return null;
    }

    @Override
    public void getVin() {

    }

    @Override
    public void getRtc() {

    }

    @Override
    public void setRtc(long rtcTime) {

    }

    @Override
    public void getPids(String pids) {

    }

    @Override
    public void getSupportedPids() {

    }

    @Override
    public void setPidsToSend(String pids, int timeInterval) {

    }

    @Override
    public void requestSnapshot() {

    }

    @Override
    public void clearDtcs() {

    }

    @Override
    public void getDtcs() {

    }

    @Override
    public void getPendingDtcs() {

    }

    @Override
    public void getFreezeFrame() {

    }

    @Override
    public void clearDeviceMemory() {

    }

    @Override
    public void resetDeviceToDefaults() {

    }

    @Override
    public void resetDevice() {

    }

    @Override
    public void createCommunicator(Context mContext) {

    }

    @Override
    public void connectToDevice(BluetoothDevice device) {

    }

    @Override
    public void sendPassiveCommand(String payload) {

    }

    @Override
    public void closeConnection() {

    }

    @Override
    public void setCommunicatorState(int state) {

    }

    @Override
    public int getCommunicatorState() {
        return 0;
    }
}
