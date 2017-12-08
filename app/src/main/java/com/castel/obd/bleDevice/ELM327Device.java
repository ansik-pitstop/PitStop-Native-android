package com.castel.obd.bleDevice;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import com.github.pires.obd.commands.control.VinCommand;
import com.pitstop.bluetooth.BluetoothDeviceManager;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Created by ishan on 2017-12-08.
 */

public class ELM327Device implements AbstractDevice {

    private BluetoothDeviceManager deviceManager;
    private BluetoothDevice bluetoothDevice;

    private BluetoothSocket socket;
    public ELM327Device(BluetoothDeviceManager manager){

    }

    private static final String TAG  = ELM327Device.class.getSimpleName();

    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

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
        Log.d(TAG, "getVin()");
        if (socket!=null){
            VinCommand vinCommand = new VinCommand();
            try {
                vinCommand.run(socket.getInputStream(), socket.getOutputStream());
            } catch (Exception e) {
                Log.d(TAG, "exceptionThrown");
                e.printStackTrace();
            }
        }

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
        this.bluetoothDevice = device;
        BluetoothSocket socket = null;
        BluetoothSocket fallbackSocket = null;
        try {
            socket = device.createRfcommSocketToServiceRecord(MY_UUID);
            socket.connect();
            this.socket = socket;

        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "There was an error while establishing Bluetooth connection. Falling back..");
            Class<?> clazz = socket.getRemoteDevice().getClass();
            Class<?>[] paramTypes = new Class<?>[]{Integer.TYPE};
            try {
                Method m = clazz.getMethod("createRfcommSocket", paramTypes);
                Object[] params = new Object[]{Integer.valueOf(1)};
                fallbackSocket = (BluetoothSocket) m.invoke(socket.getRemoteDevice(), params);
                fallbackSocket.connect();
                socket = fallbackSocket;
                this.socket = socket;

            } catch (Exception e2) {
                Log.e(TAG, "Couldn't fallback while establishing Bluetooth connection.", e2);
            }

        }
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
