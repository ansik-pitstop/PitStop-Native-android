package com.castel.obd.bleDevice;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import com.castel.obd.bluetooth.BluetoothCommunicator;
import com.elm.commands.ObdCommand;
import com.elm.commands.control.DistanceMILOnCommand;
import com.elm.commands.control.DistanceSinceCCCommand;
import com.elm.commands.control.DtcNumberCommand;
import com.elm.commands.control.PendingTroubleCodesCommand;
import com.elm.commands.control.PermanentTroubleCodesCommand;
import com.elm.commands.control.TroubleCodesCommand;
import com.elm.commands.control.VinCommand;
import com.elm.commands.engine.RPMCommand;
import com.elm.commands.fuel.FindFuelTypeCommand;
import com.elm.commands.protocol.AvailablePidsCommand;
import com.elm.commands.protocol.AvailablePidsCommand_01_20;
import com.elm.commands.protocol.AvailablePidsCommand_21_40;
import com.elm.commands.protocol.AvailablePidsCommand_41_60;
import com.elm.commands.protocol.DescribeProtocolCommand;
import com.elm.commands.protocol.EchoOffCommand;
import com.elm.commands.protocol.LineFeedOffCommand;
import com.elm.commands.protocol.ResetTroubleCodesCommand;
import com.elm.commands.protocol.SelectProtocolCommand;
import com.elm.commands.protocol.TimeoutCommand;
import com.elm.enums.ObdProtocols;
import com.pitstop.bluetooth.BluetoothCommunicatorELM327;
import com.pitstop.bluetooth.BluetoothDeviceManager;
import com.pitstop.bluetooth.OBDcommands.CalibrationIDCommand;
import com.pitstop.bluetooth.OBDcommands.CalibrationVehicleNumberCommand;
import com.pitstop.bluetooth.OBDcommands.EmissionsPIDCommand;
import com.pitstop.bluetooth.OBDcommands.HeaderOffCommand;
import com.pitstop.bluetooth.OBDcommands.HeaderOnCommand;
import com.pitstop.bluetooth.OBDcommands.OBDStandardCommand;
import com.pitstop.bluetooth.OBDcommands.StatusSinceDTCsClearedCommand;
import com.pitstop.bluetooth.OBDcommands.TimeSinceCC;
import com.pitstop.bluetooth.OBDcommands.TimeSinceMIL;
import com.pitstop.bluetooth.OBDcommands.WarmupsSinceCC;
import com.pitstop.bluetooth.dataPackages.DtcPackage;
import com.pitstop.bluetooth.dataPackages.PidPackage;
import com.pitstop.models.DebugMessage;
import com.pitstop.utils.Logger;
import com.pitstop.utils.TimeoutTimer;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

/**
 * Created by ishan on 2017-12-08.
 */

public class ELM327Device implements AbstractDevice {

    private final String TAG = getClass().getSimpleName();
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private final int PID_TIMEOUT_LENGTH = 2;
    private final int PID_TIMEOUT_RETRIES = 0;
    private BluetoothCommunicator communicator;
    private BluetoothDeviceManager manager;
    private boolean currentDtcsRequested;
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
    public void onConnectionStateChange(int state) {
        Log.d(TAG,"onConnectionStateChange() state: "+state);
        this.manager.setState(state);

        switch(state){
            case BluetoothCommunicator.CONNECTED:
                Log.d(TAG,"Setting up ELM device");
                ((BluetoothCommunicatorELM327)communicator).writeData(new EchoOffCommand());
                ((BluetoothCommunicatorELM327)communicator).writeData(new LineFeedOffCommand());
                ((BluetoothCommunicatorELM327)communicator).writeData(new TimeoutCommand(125));
                ((BluetoothCommunicatorELM327)communicator).writeData(new SelectProtocolCommand(ObdProtocols.AUTO));
                ((BluetoothCommunicatorELM327)communicator).writeData(new VinCommand());
                break;
        }
    }

    @Override
    public void requestData() {
        Log.d(TAG,"requestData()");
    }

    @Override
    public String getDeviceName() {
        Log.d(TAG,"getDeviceName()");
        return deviceName;
    }


    @Override
    public boolean getVin() {
        Log.d(TAG, "getVin()");
        if (communicator == null) return false;
        ((BluetoothCommunicatorELM327)communicator).writeData(new VinCommand());
        return true;
    }

    @Override
    public boolean getRtc() { //Todo: this method doesn't belong here
        Log.d(TAG,"getRtc()");
        //ELM devices dont have internal clock or memore so the rtc tome returned should just be current time
        manager.onGotRtc(System.currentTimeMillis() / 1000);
        return true;
    }

    @Override
    public boolean setRtc(long rtcTime) { //Todo: this method doesn't belong here
        Log.d(TAG,"setRtc() rtcTime: "+rtcTime);
        return true;
    }

    @Override
    public boolean getPids(String pids) {
        Log.d(TAG,"getPids() pids: "+pids);
        //IMPLEMENT THIS
        return false;
    }

    @Override
    public boolean getSupportedPids() {
        Log.d(TAG,"getSupportedPids()");
        //IMPLEMENT THIS
        return false;
    }

    @Override
    public boolean setPidsToSend(String pids, int timeInterval) {
        Log.d(TAG,"setPidsToSend() pids: "+pids+", timeInterval: "+timeInterval);
        //IMPLEMENT THIS
        return false;
    }

    @Override
    public boolean requestSnapshot() {
        Log.d(TAG,"requestSnapshot()");
        if (communicator == null)
            return false;
        //Todo: Remove debug code below
        //        ((BluetoothCommunicatorELM327)communicator).writeData(new RPMCommand());
//        pidCommandQueue.add(new DescribeProtocolCommand());
//        pidCommandQueue.add(new StatusSinceDTCsClearedCommand());
          pidCommandQueue.add(new HeaderOffCommand());
//        pidCommandQueue.add(new AvailablePidsCommand_01_20(true));
          pidCommandQueue.add(new EmissionsPIDCommand());
          pidCommandQueue.add(new RPMCommand(true));
//        pidCommandQueue.add(new DistanceMILOnCommand());
//        pidCommandQueue.add(new WarmupsSinceCC());
//        pidCommandQueue.add(new DistanceSinceCCCommand());
//        pidCommandQueue.add(new TimeSinceCC());
//        pidCommandQueue.add(new TimeSinceMIL());
//        pidCommandQueue.add(new CalibrationIDCommand());
//        pidCommandQueue.add(new CalibrationVehicleNumberCommand());
//        pidCommandQueue.add(new OBDStandardCommand());
//        pidCommandQueue.add(new FindFuelTypeCommand());
        start();
        return true;
    }

    @Override
    public boolean clearDtcs() {
        Log.d(TAG, "clearDtcs()");
        if (communicator==null){
            Log.d(TAG, "communicator is null ");
            return false;

        }
        ((BluetoothCommunicatorELM327)communicator).writeData(new ResetTroubleCodesCommand());
        return true;
    }

    @Override
    public boolean getDtcs() {
        Log.d(TAG, "getDtc()");
        if (communicator==null){
            Log.d(TAG, "communicator is null ");
            return false;

        }
        currentDtcsRequested = true;
        ((BluetoothCommunicatorELM327)communicator).writeData(new TroubleCodesCommand());
        return true;
    }

    @Override
    public boolean getPendingDtcs() {
        Log.d(TAG, "getPendingDtc()");
        if (communicator==null){
            Log.d(TAG, "communicator is null ");
            return false;
        }else if (currentDtcsRequested){
            return false;
        }

        ((BluetoothCommunicatorELM327)communicator).writeData(new PendingTroubleCodesCommand());
        return true;

    }

    @Override
    public boolean getFreezeFrame() {
        return false; //Implement this
    }

    @Override
    public boolean clearDeviceMemory() {
        return false; //Todo: this doesn't belong in this class
    }

    @Override
    public boolean resetDeviceToDefaults() {
        //Todo: this doesn't belong in this class
        return false;
    }

    @Override
    public boolean resetDevice() {
        //Todo: this doesn't belong in this class
        return false;
    }

    @Override
    public synchronized boolean connectToDevice(BluetoothDevice device) {

        Log.d(TAG, "connectToDevice: " + device.getName());
        this.deviceName = device.getAddress();
        if (manager.getState() == BluetoothCommunicator.CONNECTING){
            Logger.getInstance().logI(TAG,"Connecting to device: Error, already connecting/connected to a device"
                    , DebugMessage.TYPE_BLUETOOTH);
            return false;
        }

        manager.setState(BluetoothCommunicator.CONNECTING);
        Log.i(TAG, "Connecting to Classic device");
        communicator.connectToDevice(device);
        return true;
    }

    @Override
    public boolean sendPassiveCommand(String payload) {
        return false; //Implement this
    }

    @Override
    public boolean closeConnection() {
        if (communicator == null) return false;
        communicator.close();
        return true;
    }

    @Override
    public boolean setCommunicatorState(int state) {
        if (communicator!=null){
            communicator.bluetoothStateChanged(state);
            return true;
        }
        else{
            return false;
        }
    }

    @Override
    public int getCommunicatorState() {
        if (communicator == null) return BluetoothCommunicator.DISCONNECTED;
        else return communicator.getState();
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
            Log.d(TAG, "Describe Protocol: " + obdCommand.getFormattedResult());
            //pidPackage.pids.put("Protocol",  obdCommand.getFormattedResult());
            next();
        }
        else if (obdCommand instanceof StatusSinceDTCsClearedCommand){
            Log.d(TAG, "DTC number Command: " + obdCommand.getFormattedResult());
//            pidPackage.pids.put("DtcCount: ",  Integer.toString(((StatusSinceDTCsClearedCommand) obdCommand).getCodeCount()));
//            pidPackage.pids.put("MIL: ",  Boolean.toString(((StatusSinceDTCsClearedCommand) obdCommand).getMilOn()));
//            pidPackage.pids.put("IgnitionType: ",  ((StatusSinceDTCsClearedCommand) obdCommand).getIgnitionType());
            next();

        }
        else if (obdCommand instanceof EmissionsPIDCommand){
            Log.d(TAG, "Emissions PID: " + obdCommand.getCalculatedResult() +", isHeader: "+obdCommand.isHeaders());
            pidPackage.pids.put("2141",  obdCommand.getCalculatedResult());
            next();

        }
        else if (obdCommand instanceof RPMCommand){
            pidPackage.pids.put("210C", obdCommand.getCalculatedResult());
            Log.d(TAG, "rpm:  " + obdCommand.getFormattedResult());
            next();
        }
        else if (obdCommand instanceof DistanceMILOnCommand){
            Log.d(TAG, "Distance since MIL: " + obdCommand.getFormattedResult());
           // pidPackage.pids.put("DistanceSinceMIL", obdCommand.getFormattedResult());
            next();
        }
        else if (obdCommand instanceof WarmupsSinceCC){
            Log.d(TAG, "WarmupsSinceCC: " + obdCommand.getFormattedResult());
            //pidPackage.pids.put("WarmupsSinceCC", obdCommand.getFormattedResult());
            next();
        }
        else if (obdCommand instanceof DistanceSinceCCCommand){
            Log.d(TAG, "Distance since CC: " + obdCommand.getFormattedResult());
           // pidPackage.pids.put("DistanceSinceCC: ", obdCommand.getFormattedResult());
            next();
        }
        else if (obdCommand instanceof TimeSinceMIL){
            Log.d(TAG, "Time Since MIL: " + obdCommand.getFormattedResult());
            //pidPackage.pids.put("Time Since MIL: ", obdCommand.getCalculatedResult());
            next();
        }
        else if (obdCommand instanceof TimeSinceCC){
            Log.d(TAG, "Time Since CC: " + obdCommand.getFormattedResult());
           // pidPackage.pids.put("Time Since CC: ", obdCommand.getCalculatedResult());
            next();
        }
        else if (obdCommand instanceof CalibrationIDCommand){
            Log.d(TAG, "CAL ID: " + obdCommand.getFormattedResult());
           // pidPackage.pids.put("Calibration ID", obdCommand.getFormattedResult());
            next();
        }
        else if (obdCommand instanceof CalibrationVehicleNumberCommand){
            Log.d(TAG, "CVN: "+ obdCommand.getFormattedResult());
         //   pidPackage.pids.put("Calbration Vehicle Number", obdCommand.getFormattedResult());
            next();
        }
        else if (obdCommand instanceof OBDStandardCommand){
            Log.d(TAG, "OBD Standard: " + obdCommand.getFormattedResult());
           // pidPackage.pids.put("OBD Standard: ", obdCommand.getCalculatedResult());
            next();
        }

        else if (obdCommand instanceof FindFuelTypeCommand){
            Log.d(TAG, "Fuel Type: "  + obdCommand.getFormattedResult());
           // pidPackage.pids.put("Fuel Type: ", obdCommand.getFormattedResult());
            next();

        }
        else if(obdCommand instanceof AvailablePidsCommand){
            Log.d(TAG, "Available PIDS: {formatted: " + obdCommand.getFormattedResult() +", calculated: "+obdCommand.getCalculatedResult()+" }");
           // pidPackage.pids.put(obdCommand.getName(), obdCommand.getFormattedResult());
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
                obdCommand instanceof EmissionsPIDCommand ||
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
        ((BluetoothCommunicatorELM327)communicator).writeData(new AvailablePidsCommand_01_20(true));

    }
    private void getAvailablePids21_40(){
        Log.d(TAG, "availabalePIDS_21-40()");
        if (communicator==null){
            Log.d(TAG, "communicator is null ");
            return;
        }
        ((BluetoothCommunicatorELM327)communicator).writeData(new AvailablePidsCommand_21_40(true));

    }
    private void getAvailablePids41_60(){
        Log.d(TAG, "availabalePIDS_41-60()");
        if (communicator==null){
            Log.d(TAG, "communicator is null ");
            return;
        }
        ((BluetoothCommunicatorELM327)communicator).writeData(new AvailablePidsCommand_41_60(true));

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
