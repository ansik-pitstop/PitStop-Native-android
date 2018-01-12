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
import com.github.pires.obd.commands.ObdCommand;
import com.github.pires.obd.commands.control.DistanceMILOnCommand;
import com.github.pires.obd.commands.control.DistanceSinceCCCommand;
import com.github.pires.obd.commands.control.DtcNumberCommand;
import com.github.pires.obd.commands.control.IgnitionMonitorCommand;
import com.github.pires.obd.commands.control.PendingTroubleCodesCommand;
import com.github.pires.obd.commands.control.PermanentTroubleCodesCommand;
import com.github.pires.obd.commands.control.TroubleCodesCommand;
import com.github.pires.obd.commands.control.VinCommand;
import com.github.pires.obd.commands.engine.RPMCommand;
import com.github.pires.obd.commands.fuel.FindFuelTypeCommand;
import com.github.pires.obd.commands.protocol.AvailablePidsCommand;
import com.github.pires.obd.commands.protocol.AvailablePidsCommand_01_20;
import com.github.pires.obd.commands.protocol.AvailablePidsCommand_21_40;
import com.github.pires.obd.commands.protocol.AvailablePidsCommand_41_60;
import com.github.pires.obd.commands.protocol.DescribeProtocolCommand;
import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.github.pires.obd.commands.protocol.LineFeedOffCommand;
import com.github.pires.obd.commands.protocol.ResetTroubleCodesCommand;
import com.github.pires.obd.commands.protocol.SelectProtocolCommand;
import com.github.pires.obd.commands.protocol.TimeoutCommand;
import com.github.pires.obd.enums.AvailableCommandNames;
import com.github.pires.obd.enums.ObdProtocols;
import com.pitstop.bluetooth.BluetoothCommunicatorELM327;
import com.pitstop.bluetooth.BluetoothDeviceManager;
import com.pitstop.bluetooth.OBDcommands.CalibrationIDCommand;
import com.pitstop.bluetooth.OBDcommands.CalibrationVehicleNumberCommand;
import com.pitstop.bluetooth.OBDcommands.EmmisionsPIDCommand;
import com.pitstop.bluetooth.OBDcommands.HeaderOffCommand;
import com.pitstop.bluetooth.OBDcommands.HeaderOnCommand;
import com.pitstop.bluetooth.OBDcommands.OBDStandardCommand;
import com.pitstop.bluetooth.OBDcommands.StatusSinceDTCsClearedCommand;
import com.pitstop.bluetooth.OBDcommands.TimeSinceCC;
import com.pitstop.bluetooth.OBDcommands.TimeSinceMIL;
import com.pitstop.bluetooth.OBDcommands.WarmupsSinceCC;
import com.pitstop.bluetooth.dataPackages.DtcPackage;
import com.pitstop.bluetooth.dataPackages.PidPackage;
import com.pitstop.database.TABLES;
import com.pitstop.models.DebugMessage;
import com.pitstop.utils.Logger;
import com.pitstop.utils.TimeoutTimer;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import rx.functions.Function;

/**
 * Created by ishan on 2017-12-08.
 */

public class ELM327Device implements AbstractDevice {

    private final int PID_TIMEOUT_LENGTH = 2;
    private final int PID_TIMEOUT_RETRIES = 0;
    private BluetoothCommunicator communicator;
    private BluetoothDeviceManager manager;
    private boolean currentDtcsRequested;
    private final static String PIDTAG = "PID: ";
    private DtcPackage dtcPackage;
    private PidPackage pidPackage = new PidPackage();
    private String deviceName ="";
    private Queue<ObdCommand> pidCommandQueue = new LinkedList<>();
    public ELM327Device(Context mContext, BluetoothDeviceManager manager){
        this.manager  = manager;
        this.communicator = new BluetoothCommunicatorELM327(mContext, this);
    }


    private final TimeoutTimer individualPidTimeoutTimer = new TimeoutTimer(PID_TIMEOUT_LENGTH, 0 ) {
        @Override
        public void onRetry() {
            Log.d(TAG, "individualPidTimeoutTimer.onRetry");

        }

        @Override
        public void onTimeout() {
            Log.d(TAG, "individualPidTimeoutTimer.onTimeout");
            next();

        }
    };



    private void start(){
        pidPackage.pids = new HashMap<>();

        next();
    }

    private void next() {
        Log.d(TAG, "next()");
        if (pidCommandQueue.isEmpty()){
            Log.d(TAG, "queue is empty()");
            finish();
            return;
        }

        else{
            if (communicator==null){
                Log.d(TAG, "communicator is null ");
                return;

            }
            individualPidTimeoutTimer.cancel();
            individualPidTimeoutTimer.startTimer();
            ObdCommand currCommand = pidCommandQueue.peek();
            pidCommandQueue.remove(currCommand);
            ((BluetoothCommunicatorELM327)communicator).writeData(currCommand);
        }

    }

    private void finish() {
        Log.d(TAG,"finish()");

        // when the pidCommandQueeue is finished executing, it sets up the pidPackage object ,
        // sends it up to the manager to handle it and then creates a new empty pid package;
        pidPackage.deviceId = this.deviceName;

        // not sure for what to put for trip id and mileage since these devices dont really have trips
        //
        pidPackage.tripId = "1000000000000";
        pidPackage.tripMileage = "100.00";
        pidPackage.rtcTime = String.valueOf(System.currentTimeMillis() / 1000);
        pidPackage.timestamp = "timestamp";
        pidPackage.realTime = true;
        manager.gotPidPackage(pidPackage);
        pidPackage  = new PidPackage();
        pidPackage.pids = new HashMap<>();
        pidCommandQueue = new LinkedList<>();


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
        return deviceName;
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
        //ELM devices dont have internal clock or memore so the rtc tome returned should just be current time
        manager.onGotRtc(System.currentTimeMillis() / 1000);

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
        // make sure you change this
        /*Log.d(TAG, "requestSnapshot()");
        if (communicator==null){
            Log.d(TAG, "communicator is null ");
            return;

        }*/
//        ((BluetoothCommunicatorELM327)communicator).writeData(new RPMCommand());
        pidCommandQueue.add(new DescribeProtocolCommand());
        pidCommandQueue.add(new StatusSinceDTCsClearedCommand());
        pidCommandQueue.add(new HeaderOnCommand());
        pidCommandQueue.add(new AvailablePidsCommand_01_20());
        pidCommandQueue.add(new HeaderOffCommand());
        pidCommandQueue.add(new EmmisionsPIDCommand());
        pidCommandQueue.add(new RPMCommand());
        pidCommandQueue.add(new DistanceMILOnCommand());
        pidCommandQueue.add(new WarmupsSinceCC());
        pidCommandQueue.add(new DistanceSinceCCCommand());
        pidCommandQueue.add(new TimeSinceCC());
        pidCommandQueue.add(new TimeSinceMIL());
        pidCommandQueue.add(new CalibrationIDCommand());
        pidCommandQueue.add(new CalibrationVehicleNumberCommand());
        pidCommandQueue.add(new OBDStandardCommand());
        pidCommandQueue.add(new FindFuelTypeCommand());
        start();




    }

    @Override
    public void clearDtcs() {
        Log.d(TAG, "getVin()");
        if (communicator==null){
            Log.d(TAG, "communicator is null ");
            return;

        }
        ((BluetoothCommunicatorELM327)communicator).writeData(new ResetTroubleCodesCommand());


    }

    @Override
    public void getDtcs() {
        Log.d(TAG, "getDtc()");
        if (communicator==null){
            Log.d(TAG, "communicator is null ");
            return;

        }
        currentDtcsRequested = true;
        ((BluetoothCommunicatorELM327)communicator).writeData(new TroubleCodesCommand());



    }

    @Override
    public void getPendingDtcs() {
        Log.d(TAG, "getPendingDtc()");
        if (communicator==null){
            Log.d(TAG, "communicator is null ");
            return;

        }
        if (currentDtcsRequested)return;
        ((BluetoothCommunicatorELM327)communicator).writeData(new PendingTroubleCodesCommand());


    }
    int k = 0 ;
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
    public synchronized void connectToDevice(BluetoothDevice device) {

        Log.d(TAG, "connectToDevice: " + device.getName());
        this.deviceName = device.getAddress();
        if (manager.getState() == BluetoothCommunicator.CONNECTING){
            Logger.getInstance().logI(TAG,"Connecting to device: Error, already connecting/connected to a device"
                    , DebugMessage.TYPE_BLUETOOTH);
            return;
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
        Log.d(TAG, "getVin()");
        if (communicator==null){
            Log.d(TAG, "communicator is null ");
            return;

        }
        ((BluetoothCommunicatorELM327)communicator).writeData(new EchoOffCommand());
        ((BluetoothCommunicatorELM327)communicator).writeData(new LineFeedOffCommand());
        ((BluetoothCommunicatorELM327)communicator).writeData(new TimeoutCommand(125));
        ((BluetoothCommunicatorELM327)communicator).writeData(new SelectProtocolCommand(ObdProtocols.AUTO));
        ((BluetoothCommunicatorELM327)communicator).writeData(new VinCommand());

    }

    public void parseData(ObdCommand obdCommand) {
        if (obdCommand.getName()!=null)
            Log.w(TAG, "Got obd Result for command: " + obdCommand.getName() + " : "  + obdCommand.getFormattedResult()) ;
        if(obdCommand instanceof VinCommand)
            manager.onGotVin(obdCommand.getFormattedResult(), this.deviceName);
        else if(obdCommand instanceof TroubleCodesCommand){
            String[] dtcsFromDevice = obdCommand.getFormattedResult().split("\n");
            if (dtcPackage== null)
                dtcPackage = new DtcPackage();
            dtcPackage.deviceId = deviceName;
            dtcPackage.rtcTime = String.valueOf(System.currentTimeMillis() / 1000);
            if (dtcPackage.dtcs == null)
                dtcPackage.dtcs = new HashMap<>();
            for (String DtcsFromDevice : dtcsFromDevice) {
                dtcPackage.dtcs.put(DtcsFromDevice, false);
            }
            currentDtcsRequested = false;
            getPendingDtcs();
        }
        else if (obdCommand instanceof PendingTroubleCodesCommand){
            String[] dtcsFromDevice = obdCommand.getFormattedResult().split("\n");
            if (dtcPackage== null)
                dtcPackage = new DtcPackage();
            if (dtcPackage.dtcs == null)
                dtcPackage.dtcs = new HashMap<>();
            dtcPackage.rtcTime = String.valueOf(System.currentTimeMillis() / 1000);
            for (String DtcsFromDevice : dtcsFromDevice) {
                dtcPackage.dtcs.put(DtcsFromDevice, true);
            }
            manager.gotDtcData(dtcPackage);
            dtcPackage = null;
        }
        else if (obdCommand instanceof DescribeProtocolCommand){
            Log.d(PIDTAG, "Describe Protocol: " + obdCommand.getFormattedResult());
            pidPackage.pids.put("Protocol",  obdCommand.getFormattedResult());
            next();
        }
        else if (obdCommand instanceof StatusSinceDTCsClearedCommand){
            Log.d(PIDTAG, "DTC number Command: " + obdCommand.getFormattedResult());
            pidPackage.pids.put("DtcCount: ",  Integer.toString(((StatusSinceDTCsClearedCommand) obdCommand).getCodeCount()));
            pidPackage.pids.put("MIL: ",  Boolean.toString(((StatusSinceDTCsClearedCommand) obdCommand).getMilOn()));
            pidPackage.pids.put("IgnitionType: ",  ((StatusSinceDTCsClearedCommand) obdCommand).getIgnitionType());
            next();

        }
        else if (obdCommand instanceof EmmisionsPIDCommand){
            Log.d(PIDTAG, "Emmisions PID: " + obdCommand.getFormattedResult());
            String pidValue = obdCommand.getFormattedResult().substring(obdCommand.getFormattedResult().length()-8);
            pidPackage.pids.put("2141",  pidValue);
            next();

        }
        else if (obdCommand instanceof RPMCommand){
            pidPackage.pids.put("210C", obdCommand.getCalculatedResult());
            Log.d(PIDTAG, "rpm:  " + obdCommand.getFormattedResult());
            next();
        }
        else if (obdCommand instanceof DistanceMILOnCommand){
            Log.d(PIDTAG, "Distance since MIL: " + obdCommand.getFormattedResult());
            pidPackage.pids.put("DistanceSinceMIL", obdCommand.getFormattedResult());
            next();
        }
        else if (obdCommand instanceof WarmupsSinceCC){
            Log.d(PIDTAG, "WarmupsSinceCC: " + obdCommand.getFormattedResult());
            pidPackage.pids.put("WarmupsSinceCC", obdCommand.getFormattedResult());
            next();
        }
        else if (obdCommand instanceof DistanceSinceCCCommand){
            Log.d(PIDTAG, "Distance since CC: " + obdCommand.getFormattedResult());
            pidPackage.pids.put("DistanceSinceCC: ", obdCommand.getFormattedResult());
            next();
        }
        else if (obdCommand instanceof TimeSinceMIL){
            Log.d(PIDTAG, "Time Since MIL: " + obdCommand.getFormattedResult());
            pidPackage.pids.put("Time Since MIL: ", obdCommand.getCalculatedResult());
            next();
        }
        else if (obdCommand instanceof TimeSinceCC){
            Log.d(PIDTAG, "Time Since CC: " + obdCommand.getFormattedResult());
            pidPackage.pids.put("Time Since CC: ", obdCommand.getCalculatedResult());
            next();
        }
        else if (obdCommand instanceof CalibrationIDCommand){
            Log.d(PIDTAG, "CAL ID: " + obdCommand.getFormattedResult());
            pidPackage.pids.put("Calibration ID", obdCommand.getFormattedResult());
            next();
        }
        else if (obdCommand instanceof CalibrationVehicleNumberCommand){
            Log.d(PIDTAG, "CVN: "+ obdCommand.getFormattedResult());
            pidPackage.pids.put("Calbration Vehicle Number", obdCommand.getFormattedResult());
            next();
        }
        else if (obdCommand instanceof OBDStandardCommand){
            Log.d(PIDTAG, "OBD Standard: " + obdCommand.getFormattedResult());
            pidPackage.pids.put("OBD Standard: ", obdCommand.getCalculatedResult());
            next();
        }

        else if (obdCommand instanceof FindFuelTypeCommand){
            Log.d(PIDTAG, "Fuel Type: "  + obdCommand.getFormattedResult());
            pidPackage.pids.put("Fuel Type: ", obdCommand.getFormattedResult());
            next();

        }
        else if(obdCommand instanceof AvailablePidsCommand){
            Log.d(PIDTAG, "Available PIDS: " + obdCommand.getFormattedResult());
            pidPackage.pids.put(obdCommand.getName(), obdCommand.getFormattedResult());
            next();
        }





    }

    public void noData(ObdCommand obdCommand) {
        if (obdCommand instanceof TroubleCodesCommand){
            if (dtcPackage== null)
                dtcPackage = new DtcPackage();
            dtcPackage.deviceId = this.deviceName;
            dtcPackage.rtcTime = String.valueOf(System.currentTimeMillis() / 1000);
            if (dtcPackage.dtcs == null)
                dtcPackage.dtcs = new HashMap<>();
            currentDtcsRequested = false;
            getPendingDtcs();
        }
        else if (obdCommand instanceof PendingTroubleCodesCommand){
            if (dtcPackage!=null)
                manager.gotDtcData(dtcPackage);
        }
        if (obdCommand instanceof DescribeProtocolCommand ||
                obdCommand instanceof StatusSinceDTCsClearedCommand||
                obdCommand instanceof EmmisionsPIDCommand ||
                obdCommand instanceof RPMCommand||
                obdCommand instanceof DistanceMILOnCommand ||
                obdCommand instanceof DistanceSinceCCCommand ||
                obdCommand instanceof TimeSinceCC ||
                obdCommand instanceof TimeSinceMIL ||
                obdCommand instanceof CalibrationVehicleNumberCommand ||
                obdCommand instanceof CalibrationIDCommand ||
                obdCommand instanceof OBDStandardCommand ||
                obdCommand instanceof FindFuelTypeCommand ||
                obdCommand instanceof HeaderOnCommand ||
                obdCommand instanceof  HeaderOffCommand||
                obdCommand instanceof AvailablePidsCommand)
            next();

    }

    private void getProtocol(){
        Log.d(TAG, "getProtocol()");
        if (communicator==null){
            Log.d(TAG, "communicator is null ");
            return;

        }
        ((BluetoothCommunicatorELM327)communicator).writeData(new DescribeProtocolCommand());
    }

    private void checkEngineLight(){
        Log.d(TAG, "checkEngineLight()");
        if (communicator==null){
            Log.d(TAG, "communicator is null ");
            return;
        }
        ((BluetoothCommunicatorELM327)communicator).writeData(new DtcNumberCommand());
    }

    private void getAvailablePids1_20(){
        Log.d(TAG, "availabalePIDS_1-20()");
        if (communicator==null){
            Log.d(TAG, "communicator is null ");
            return;
        }
        ((BluetoothCommunicatorELM327)communicator).writeData(new AvailablePidsCommand_01_20());

    }
    private void getAvailablePids21_40(){
        Log.d(TAG, "availabalePIDS_21-40()");
        if (communicator==null){
            Log.d(TAG, "communicator is null ");
            return;
        }
        ((BluetoothCommunicatorELM327)communicator).writeData(new AvailablePidsCommand_21_40());

    }
    private void getAvailablePids41_60(){
        Log.d(TAG, "availabalePIDS_41-60()");
        if (communicator==null){
            Log.d(TAG, "communicator is null ");
            return;
        }
        ((BluetoothCommunicatorELM327)communicator).writeData(new AvailablePidsCommand_41_60());

    }
    private void getPermanentDtcs(){
        Log.d(TAG, "permanentDtcCommand");
        if (communicator==null){
            Log.d(TAG, "communicator is null ");
            return;
        }
        ((BluetoothCommunicatorELM327)communicator).writeData(new PermanentTroubleCodesCommand());
    }
    private void distanceSinceMIL(){
        Log.d(TAG, "distanceSinceMIL");
        if (communicator==null){
            Log.d(TAG, "communicator is null ");
            return;
        }
        ((BluetoothCommunicatorELM327)communicator).writeData(new DistanceMILOnCommand());
    }

    private void distanceSinceCC(){
        Log.d(TAG, "distanceSinceCodesCleared");
        if (communicator==null){
            Log.d(TAG, "communicator is null ");
            return;
        }
        ((BluetoothCommunicatorELM327)communicator).writeData(new DistanceSinceCCCommand());

    }

    private void getFuelType(){
        Log.d(TAG, "getFuelType");
        if (communicator==null){
            Log.d(TAG, "communicator is null ");
            return;
        }
        ((BluetoothCommunicatorELM327)communicator).writeData(new FindFuelTypeCommand());


    }


}
