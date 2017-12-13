package com.castel.obd.bleDevice;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.castel.obd.bluetooth.BluetoothChatElm327;
import com.castel.obd.bluetooth.BluetoothCommunicator;
import com.castel.obd.bluetooth.BluetoothLeComm;
import com.castel.obd.bluetooth.IBluetoothCommunicator;
import com.github.pires.obd.commands.control.VinCommand;
import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.github.pires.obd.commands.protocol.LineFeedOffCommand;
import com.github.pires.obd.commands.protocol.SelectProtocolCommand;
import com.github.pires.obd.commands.protocol.TimeoutCommand;
import com.github.pires.obd.enums.ObdProtocols;
import com.pitstop.bluetooth.BluetoothCommunicatorELM327;
import com.pitstop.bluetooth.BluetoothDeviceManager;
import com.pitstop.models.DebugMessage;
import com.pitstop.utils.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ishan on 2017-12-08.
 */

public class ELM327Device implements AbstractDevice {

    private BluetoothCommunicator communicator;
    private BluetoothDeviceManager manager;

    public ELM327Device(Context mContext, BluetoothDeviceManager manager){
        this.manager  = manager;
        this.communicator = new BluetoothCommunicatorELM327(mContext, this);

    }

    private static final String TAG  = ELM327Device.class.getSimpleName();

    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Override
    public UUID getServiceUuid() {
        return MY_UUID;

    }

    @Override
    public UUID getReadChar() {
        return MY_UUID;

    }

    @Override
    public UUID getWriteChar(){return MY_UUID;}


    @Override
    public BluetoothDeviceManager.CommType commType() {
        return null;
    }

    @Override
    public byte[] getBytes(String payload) {
        return payload.getBytes();
    }

    @Override
    public void parseData(byte[] data) {
        Log.d(TAG, data.toString());

    }

    @Override
    public void setManagerState(int state) {
        this.manager.setState(state);
    }

    @Override
    public void requestData() {

    }

    @Override
    public String getDeviceName() {
        return "Carista";
    }



    @Override
    public void getVin() {
        Log.d(TAG, "getVin()");
        if (communicator==null){
            Log.d(TAG, "communicator is null ");
            return;

        }
        ((BluetoothCommunicatorELM327)communicator).writeData(new VinCommand());
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
    public void connectToDevice(BluetoothDevice device) {
        Log.d(TAG, "connectToDevice: " + device.getName());
        if (manager.getState() == BluetoothCommunicator.CONNECTING){
            Logger.getInstance().logI(TAG,"Connecting to device: Error, already connecting/connected to a device"
                    , DebugMessage.TYPE_BLUETOOTH);
            return;
        } else if (communicator != null && manager.getState() == BluetoothCommunicator.CONNECTED){
            communicator.close();
        }

        manager.setState(BluetoothCommunicator.CONNECTING);
        Log.i(TAG, "Connecting to Classic device");
        communicator.connectToDevice(device);


    }

    @Override
    public void sendPassiveCommand(String payload) {

    }

    @Override
    public void closeConnection() {
        communicator.close();

    }

    @Override
    public void setCommunicatorState(int state) {
        if (communicator!=null)
            communicator.bluetoothStateChanged(state);
    }

    @Override
    public int getCommunicatorState() {
        return communicator.getState();
    }


    private void setUpDevice(){
        ((BluetoothCommunicatorELM327)communicator).writeData(new EchoOffCommand());
        ((BluetoothCommunicatorELM327)communicator).writeData(new LineFeedOffCommand());
        ((BluetoothCommunicatorELM327)communicator).writeData(new TimeoutCommand(125));
        ((BluetoothCommunicatorELM327)communicator).writeData(new SelectProtocolCommand(ObdProtocols.AUTO));
        ((BluetoothCommunicatorELM327)communicator).writeData(new VinCommand());

    }


    public void parseData(String obj) {
        Log.w(TAG, "I am Parsing Data " + obj);
        if (obj.length() == 17){
            manager.onGotVin(obj);
        }

    }
}
