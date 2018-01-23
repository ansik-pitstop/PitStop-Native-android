package com.pitstop.bluetooth.bleDevice;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import com.pitstop.bluetooth.communicator.BluetoothCommunicator;
import com.pitstop.bluetooth.elm.commands.ObdCommand;
import com.pitstop.bluetooth.elm.commands.control.DistanceMILOnCommand;
import com.pitstop.bluetooth.elm.commands.control.DistanceSinceCCCommand;
import com.pitstop.bluetooth.elm.commands.control.DtcNumberCommand;
import com.pitstop.bluetooth.elm.commands.control.PendingTroubleCodesCommand;
import com.pitstop.bluetooth.elm.commands.control.PermanentTroubleCodesCommand;
import com.pitstop.bluetooth.elm.commands.control.TroubleCodesCommand;
import com.pitstop.bluetooth.elm.commands.control.VinCommand;
import com.pitstop.bluetooth.elm.commands.engine.RPMCommand;
import com.pitstop.bluetooth.elm.commands.fuel.FindFuelTypeCommand;
import com.pitstop.bluetooth.elm.commands.protocol.AvailablePidsCommand;
import com.pitstop.bluetooth.elm.commands.protocol.AvailablePidsCommand_01_20;
import com.pitstop.bluetooth.elm.commands.protocol.AvailablePidsCommand_21_40;
import com.pitstop.bluetooth.elm.commands.protocol.AvailablePidsCommand_41_60;
import com.pitstop.bluetooth.elm.commands.protocol.DescribeProtocolCommand;
import com.pitstop.bluetooth.elm.commands.protocol.EchoOffCommand;
import com.pitstop.bluetooth.elm.commands.protocol.LineFeedOffCommand;
import com.pitstop.bluetooth.elm.commands.protocol.ResetTroubleCodesCommand;
import com.pitstop.bluetooth.elm.commands.protocol.SelectProtocolCommand;
import com.pitstop.bluetooth.elm.commands.protocol.TimeoutCommand;
import com.pitstop.bluetooth.elm.enums.ObdProtocols;
import com.pitstop.bluetooth.BluetoothCommunicatorELM327;
import com.pitstop.bluetooth.BluetoothDeviceManager;
import com.pitstop.bluetooth.elm.commands.other.CalibrationIDCommand;
import com.pitstop.bluetooth.elm.commands.other.CalibrationVehicleNumberCommand;
import com.pitstop.bluetooth.elm.commands.other.EmissionsPIDCommand;
import com.pitstop.bluetooth.elm.commands.other.HeaderOffCommand;
import com.pitstop.bluetooth.elm.commands.other.HeaderOnCommand;
import com.pitstop.bluetooth.elm.commands.other.OBDStandardCommand;
import com.pitstop.bluetooth.elm.commands.other.StatusSinceDTCsClearedCommand;
import com.pitstop.bluetooth.elm.commands.other.TimeSinceCC;
import com.pitstop.bluetooth.elm.commands.other.TimeSinceMIL;
import com.pitstop.bluetooth.elm.commands.other.WarmupsSinceCC;
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
    private PidPackage pidPackage = new PidPackage();
    private String deviceName ="";
    private boolean headersEnabled = false;
    private Queue<ObdCommand> pidCommandQueue = new LinkedList<>();
    private ObdProtocols obdProtocol = null;

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
            //Setup device once connected
            case BluetoothCommunicator.CONNECTED:
                Log.d(TAG,"Setting up ELM device");
                ((BluetoothCommunicatorELM327)communicator).writeData(new SelectProtocolCommand(ObdProtocols.AUTO));
                ((BluetoothCommunicatorELM327)communicator).writeData(new DescribeProtocolCommand()); //On the receival of this command the protocol will be set
                ((BluetoothCommunicatorELM327)communicator).writeData(new EchoOffCommand());
                ((BluetoothCommunicatorELM327)communicator).writeData(new LineFeedOffCommand());
                ((BluetoothCommunicatorELM327)communicator).writeData(new TimeoutCommand(125));
                setHeaders(false); //Headers on by default
                ((BluetoothCommunicatorELM327)communicator).writeData(new VinCommand(false));
                break;
            case BluetoothCommunicator.DISCONNECTED:
                obdProtocol = null;
                headersEnabled = false;
                break;
        }
    }

    @Override
    public void requestData() {
        Log.d(TAG,"requestData()");
    }

    @Override
    public boolean getVin() {
        Log.d(TAG, "getVin()");
        if (communicator == null) return false;
        ((BluetoothCommunicatorELM327)communicator).writeData(new VinCommand(false));
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
        setHeaders(false);
        //        ((BluetoothCommunicatorELM327)communicator).writeData(new RPMCommand());
          pidCommandQueue.add(new DescribeProtocolCommand());
//        pidCommandQueue.add(new StatusSinceDTCsClearedCommand());
//        pidCommandQueue.add(new AvailablePidsCommand_01_20(true));
          pidCommandQueue.add(new EmissionsPIDCommand(headersEnabled));
//          pidCommandQueue.add(new RPMCommand(true));
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
        ((BluetoothCommunicatorELM327)communicator).writeData(new ResetTroubleCodesCommand(headersEnabled));
        return true;
    }

    @Override
    public boolean getDtcs() {
        Log.d(TAG, "getDtc()");
        if (communicator==null || obdProtocol == null){
            Log.d(TAG, "communicator is null ");
            return false;

        }

        setHeaders(false);
        currentDtcsRequested = true;
        ((BluetoothCommunicatorELM327)communicator).writeData(new TroubleCodesCommand(obdProtocol,headersEnabled));
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

        ((BluetoothCommunicatorELM327)communicator).writeData(new PendingTroubleCodesCommand(obdProtocol,headersEnabled));
        return true;

    }

    @Override
    public boolean getFreezeFrame() {
        return false; //Implement this
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
            String[] dtcsFromDevice = obdCommand.getCalculatedResult().split("\n");
            DtcPackage dtcPackage = new DtcPackage(deviceName
                    ,String.valueOf(System.currentTimeMillis()/1000), new HashMap<>());
            for (String DtcsFromDevice : dtcsFromDevice) {
                dtcPackage.dtcs.put(DtcsFromDevice, false);
            }
            manager.gotDtcData(dtcPackage);
            currentDtcsRequested = false;
            getPendingDtcs();
        }
        else if (obdCommand instanceof PendingTroubleCodesCommand){
            String[] dtcsFromDevice = obdCommand.getCalculatedResult().split("\n");
            DtcPackage dtcPackage = new DtcPackage(deviceName
                    ,String.valueOf(System.currentTimeMillis()/1000), new HashMap<>());
            for (String DtcsFromDevice : dtcsFromDevice) {
                dtcPackage.dtcs.put(DtcsFromDevice, true);
            }
            manager.gotDtcData(dtcPackage);
        }
        else if (obdCommand instanceof DescribeProtocolCommand){
            Log.d(TAG, "Describe Protocol: " + obdCommand.getFormattedResult());
            setObdProtocol(obdCommand.getFormattedResult());
            next();
        }
        else if (obdCommand instanceof StatusSinceDTCsClearedCommand){
            Log.d(TAG, "DTC number Command: " + obdCommand.getFormattedResult());
            next();

        }
        else if (obdCommand instanceof EmissionsPIDCommand){
            Log.d(TAG, "Emissions PID: " + obdCommand.getCalculatedResult() +", isHeader: "+headersEnabled);
            pidPackage.pids.put("2141",  obdCommand.getData().get(0));
            next();
        }
        else if (obdCommand instanceof RPMCommand){
            pidPackage.pids.put("210C", obdCommand.getData().get(0));
            Log.d(TAG, "rpm:  " + obdCommand.getData().get(0));
            next();
        }
        else if (obdCommand instanceof DistanceMILOnCommand){
            Log.d(TAG, "Distance since MIL: " + obdCommand.getFormattedResult());
            next();
        }
        else if (obdCommand instanceof WarmupsSinceCC){
            Log.d(TAG, "WarmupsSinceCC: " + obdCommand.getFormattedResult());
            next();
        }
        else if (obdCommand instanceof DistanceSinceCCCommand){
            Log.d(TAG, "Distance since CC: " + obdCommand.getFormattedResult());
            next();
        }
        else if (obdCommand instanceof TimeSinceMIL){
            Log.d(TAG, "Time Since MIL: " + obdCommand.getFormattedResult());
            next();
        }
        else if (obdCommand instanceof TimeSinceCC){
            Log.d(TAG, "Time Since CC: " + obdCommand.getFormattedResult());
            next();
        }
        else if (obdCommand instanceof CalibrationIDCommand){
            Log.d(TAG, "CAL ID: " + obdCommand.getFormattedResult());
            next();
        }
        else if (obdCommand instanceof CalibrationVehicleNumberCommand){
            Log.d(TAG, "CVN: "+ obdCommand.getFormattedResult());
            next();
        }
        else if (obdCommand instanceof OBDStandardCommand){
            Log.d(TAG, "OBD Standard: " + obdCommand.getFormattedResult());
            next();
        }

        else if (obdCommand instanceof FindFuelTypeCommand){
            Log.d(TAG, "Fuel Type: "  + obdCommand.getFormattedResult());
            next();

        }
        else if(obdCommand instanceof AvailablePidsCommand){
            Log.d(TAG, "Available PIDS: {formatted: " + obdCommand.getFormattedResult() +", calculated: "+obdCommand.getCalculatedResult()+" }");
            next();
        }
    }

    public void noData(ObdCommand obdCommand) {
        if (obdCommand instanceof TroubleCodesCommand){
            //We need to do this otherwise timeout will occur and error will be prompted to the user
            manager.gotDtcData(new DtcPackage(deviceName
                    ,String.valueOf(System.currentTimeMillis()/1000), new HashMap<>()));
            getPendingDtcs();
        }
        else if (obdCommand instanceof PendingTroubleCodesCommand){
            manager.gotDtcData(new DtcPackage(deviceName
                    ,String.valueOf(System.currentTimeMillis()/1000), new HashMap<>()));
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

    public boolean requestDescribeProtocol(){
        Log.d(TAG, "requestDescribeProtocol()");
        if (communicator==null){
            Log.d(TAG, "communicator is null ");
            return false;

        }
        ((BluetoothCommunicatorELM327)communicator).writeData(new DescribeProtocolCommand());
        return true;
    }

    public boolean request2141PID(){
        Log.d(TAG, "request2141PID()");
        if (communicator==null){
            Log.d(TAG, "communicator is null ");
            return false;
        }
        ((BluetoothCommunicatorELM327)communicator).writeData(new EmissionsPIDCommand(headersEnabled));
        return true;
    }

    public boolean requestPendingTroubleCodes(){
        Log.d(TAG, "requestPendingTroubleCodes()");
        if (communicator==null){
            Log.d(TAG, "communicator is null ");
            return false;
        }else if (obdProtocol == null){
            Log.d(TAG,"obd protocol null cannot process pending trouble codes request");
            return false;
        }
        ((BluetoothCommunicatorELM327)communicator).writeData(new PendingTroubleCodesCommand(obdProtocol,headersEnabled));
        return true;
    }

    public boolean requestStoredTroubleCodes(){
        Log.d(TAG, "requestStoredTroubleCodes()");
        if (communicator==null){
            Log.d(TAG, "communicator is null ");
            return false;
        }else if (obdProtocol == null){
            Log.d(TAG,"obd protocol is null");
            return false;
        }
        ((BluetoothCommunicatorELM327)communicator).writeData(new TroubleCodesCommand(obdProtocol,headersEnabled));
        return true;
    }

    public boolean requestSelectProtocol(ObdProtocols p){
        Log.d(TAG, "requestSelectProtocol() protocol: "+p);
        if (communicator==null){
            Log.d(TAG, "communicator is null ");
            return false;

        }
        ((BluetoothCommunicatorELM327)communicator).writeData(new SelectProtocolCommand(p));
        return true;
    }

    private boolean checkEngineLight(){
        Log.d(TAG, "checkEngineLight()");
        if (communicator==null){
            Log.d(TAG, "communicator is null ");
            return false;
        }
        ((BluetoothCommunicatorELM327)communicator).writeData(new DtcNumberCommand(headersEnabled));
        return true;
    }

    private boolean getAvailablePids1_20(){
        Log.d(TAG, "availabalePIDS_1-20()");
        if (communicator==null){
            Log.d(TAG, "communicator is null ");
            return false;
        }
        ((BluetoothCommunicatorELM327)communicator).writeData(new AvailablePidsCommand_01_20(true));
        return true;
    }
    private boolean getAvailablePids21_40(){
        Log.d(TAG, "availabalePIDS_21-40()");
        if (communicator==null){
            Log.d(TAG, "communicator is null ");
            return false;
        }
        ((BluetoothCommunicatorELM327)communicator).writeData(new AvailablePidsCommand_21_40(true));
        return true;
    }
    private boolean getAvailablePids41_60(){
        Log.d(TAG, "availabalePIDS_41-60()");
        if (communicator==null){
            Log.d(TAG, "communicator is null ");
            return false;
        }
        ((BluetoothCommunicatorELM327)communicator).writeData(new AvailablePidsCommand_41_60(true));
        return true;
    }
    private boolean getPermanentDtcs(){
        Log.d(TAG, "permanentDtcCommand");
        if (communicator==null){
            Log.d(TAG, "communicator is null ");
            return false;
        }
        ((BluetoothCommunicatorELM327)communicator).writeData(new PermanentTroubleCodesCommand(headersEnabled));
        return true;
    }

    private boolean distanceSinceMIL(){
        Log.d(TAG, "distanceSinceMIL");
        if (communicator==null){
            Log.d(TAG, "communicator is null ");
            return false;
        }
        ((BluetoothCommunicatorELM327)communicator).writeData(new DistanceMILOnCommand(headersEnabled));
        return true;
    }

    private boolean distanceSinceCC(){
        Log.d(TAG, "distanceSinceCodesCleared");
        if (communicator==null){
            Log.d(TAG, "communicator is null ");
            return false;
        }
        ((BluetoothCommunicatorELM327)communicator).writeData(new DistanceSinceCCCommand(headersEnabled));
        return true;
    }

    private boolean getFuelType(){
        Log.d(TAG, "getFuelType");
        if (communicator==null){
            Log.d(TAG, "communicator is null ");
            return false;
        }
        ((BluetoothCommunicatorELM327)communicator).writeData(new FindFuelTypeCommand(headersEnabled));
        return true;
    }

    private boolean setHeaders(boolean enabled){
        Log.d(TAG, "enableHeaders");
        if (communicator==null){
            Log.d(TAG, "communicator is null ");
            return false;
        }
        if (enabled){
            ((BluetoothCommunicatorELM327)communicator).writeData(new HeaderOnCommand());
        }else{
            ((BluetoothCommunicatorELM327)communicator).writeData(new HeaderOffCommand());
        }
        headersEnabled = enabled;
        return true;
    }

    private void setObdProtocol(String protocolDescription){
        Log.d(TAG,"setObdProtocol() protocolDescription: "+protocolDescription);

        if (protocolDescription.contains(ObdProtocolNames.SAE_J1850_PMW)){
            obdProtocol = ObdProtocols.SAE_J1850_PWM;
        }else if (protocolDescription.contains(ObdProtocolNames.SAE_J1850_VPW)){
            obdProtocol = ObdProtocols.SAE_J1850_VPW;
        }else if (protocolDescription.contains(ObdProtocolNames.ISO_9141_2)){
            obdProtocol = ObdProtocols.ISO_9141_2;
        }else if (protocolDescription.contains(ObdProtocolNames.ISO_14230_4_KPW)){
            obdProtocol = ObdProtocols.ISO_14230_4_KWP;
        }else if (protocolDescription.contains(ObdProtocolNames.ISO_14230_4_KPW_FAST)){
            obdProtocol = ObdProtocols.ISO_14230_4_KWP_FAST;
        }else if (protocolDescription.contains(ObdProtocolNames.ISO_15765_4_CAN)){
            obdProtocol = ObdProtocols.ISO_15765_4_CAN;
        }else if (protocolDescription.contains(ObdProtocolNames.ISO_15765_4_CAN_B)){
            obdProtocol = ObdProtocols.ISO_15765_4_CAN_B;
        }else if (protocolDescription.contains(ObdProtocolNames.ISO_15765_4_CAN_C)){
            obdProtocol = ObdProtocols.ISO_15765_4_CAN_C;
        }else if (protocolDescription.contains(ObdProtocolNames.ISO_15765_4_CAN_D)){
            obdProtocol = ObdProtocols.ISO_15765_4_CAN_D;
        }else if (protocolDescription.contains(ObdProtocolNames.SAE_J1939_CAN)){
            obdProtocol = ObdProtocols.SAE_J1939_CAN;
        }else{
            obdProtocol = ObdProtocols.UNKNOWN;
        }

        Log.d(TAG,"Protocol set to: "+obdProtocol);
    }

    public interface ObdProtocolNames{
        String UNKNOWN = "Unknown";
        String SAE_J1850_PMW = "SAE J1850 PWM";
        String SAE_J1850_VPW = "SAE J1850 VPW";
        String ISO_9141_2 = "ISO 9141-2";
        String ISO_14230_4_KPW = "ISO 14230-4 (KWP 5BAUD)";
        String ISO_14230_4_KPW_FAST = "ISO 14230-4 (KWP FAST)";
        String ISO_15765_4_CAN = "ISO 15765-4 (CAN 11/500)";
        String ISO_15765_4_CAN_B = "ISO 15765-4 (CAN 29/500)";
        String ISO_15765_4_CAN_C = "ISO 15765-4 (CAN 11/250)";
        String ISO_15765_4_CAN_D = "ISO 15765-4 (CAN 29/250)";
        String SAE_J1939_CAN = "SAE J1939 (CAN 29/250)";
    }
}
