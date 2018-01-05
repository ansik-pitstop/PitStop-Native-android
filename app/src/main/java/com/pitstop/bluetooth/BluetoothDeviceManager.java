package com.pitstop.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.util.Log;

import com.castel.obd.bleDevice.AbstractDevice;
import com.castel.obd.bleDevice.Device212B;
import com.castel.obd.bleDevice.Device215B;
import com.castel.obd.bluetooth.BluetoothClassicComm;
import com.castel.obd.bluetooth.BluetoothCommunicator;
import com.castel.obd.bluetooth.BluetoothLeComm;
import com.castel.obd.bluetooth.IBluetoothCommunicator;
import com.castel.obd.bluetooth.ObdManager;
import com.pitstop.application.GlobalApplication;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.get.GetPrevIgnitionTimeUseCase;
import com.pitstop.models.DebugMessage;
import com.pitstop.network.RequestError;
import com.pitstop.utils.Logger;
import com.pitstop.utils.MixpanelHelper;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Ben!
 */
public class BluetoothDeviceManager implements ObdManager.IPassiveCommandListener {

    private static final String TAG = BluetoothDeviceManager.class.getSimpleName();

    private Context mContext;
    private GlobalApplication application;
    private MixpanelHelper mixpanelHelper;
    private ObdManager.IBluetoothDataListener dataListener;

    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler = new Handler();

    private AbstractDevice deviceInterface;
    private BluetoothCommunicator communicator;
    private UseCaseComponent useCaseComponent;

    private int discoveryNum = 0;
    private int btConnectionState = BluetoothCommunicator.DISCONNECTED;
    private boolean nonUrgentScanInProgress = false;
    private boolean discoveryWasStarted = false;

    public enum CommType {
        CLASSIC, LE
    }

    public BluetoothDeviceManager(Context context) {

        mContext = context;
        application = (GlobalApplication) context.getApplicationContext();
        mixpanelHelper = new MixpanelHelper(application);

        useCaseComponent = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(mContext))
                .build();

        final BluetoothManager bluetoothManager =
                (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // for classic discovery
        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        context.registerReceiver(receiver, intentFilter);

    }

    public void setBluetoothDataListener(ObdManager.IBluetoothDataListener dataListener) {
        Logger.getInstance().logV(TAG,"setBluetoothDataListener()"
                , DebugMessage.TYPE_BLUETOOTH);
        this.dataListener = dataListener;
    }

    //Returns false if search didn't begin again
    private boolean ignoreVerification = false;

    public synchronized boolean startScan(boolean urgent, boolean ignoreVerification) {
        Logger.getInstance().logV(TAG,"startScan() urgent: " + Boolean.toString(urgent)
                        + " ignoreVerification: " + Boolean.toString(ignoreVerification)
                , DebugMessage.TYPE_BLUETOOTH);

        this.ignoreVerification = ignoreVerification;
        if (!mBluetoothAdapter.isEnabled() && !urgent) {
            Log.i(TAG, "Scan unable to start, bluetooth is disabled and non urgent scan");
            return false;
        }
        else if (!mBluetoothAdapter.isEnabled() && urgent){
            Log.d(TAG, "urgent scan, bluetooth is enabled()");
            mBluetoothAdapter.enable(); //Enable bluetooth, scan will be triggered someplace else
            return false;
        }

        if (mBluetoothAdapter.isDiscovering()) {
            Log.i(TAG, "Already scanning");
            return false;
        }

        return connectBluetooth(urgent);
    }

    public synchronized void onConnectDeviceValid(){
        Logger.getInstance().logV(TAG,"onConnectDeviceValid()"
                , DebugMessage.TYPE_BLUETOOTH);

        if (mBluetoothAdapter.isEnabled() && mBluetoothAdapter.isDiscovering()){
            Log.i(TAG,"Stopping scan");
            mBluetoothAdapter.cancelDiscovery();
            dataListener.scanFinished();
        }
    }

    @Override
    public void sendCommandPassive(String payload) {
        Logger.getInstance().logV(TAG,"sendCommandPassive()"
                , DebugMessage.TYPE_BLUETOOTH);

        if (btConnectionState != BluetoothCommunicator.CONNECTED) {
            return;
        }

        writeToObd(payload);
    }

    public void close() {
        Logger.getInstance().logV(TAG,"close()"
                , DebugMessage.TYPE_BLUETOOTH);

        btConnectionState = IBluetoothCommunicator.DISCONNECTED;
        if (mBluetoothAdapter != null && mBluetoothAdapter.isDiscovering()){
            mBluetoothAdapter.cancelDiscovery();
            dataListener.scanFinished();
        }

        if (communicator != null ) {
            communicator.close();
        }
        try {
            mContext.unregisterReceiver(receiver);
        } catch (Exception e) {
            Log.d(TAG, "Receiver not registered");
        }
    }

    private void writeToObd(String payload) {
        Logger.getInstance().logV(TAG,"writeToObd() payload: "+payload+ ", communicator null ? "
                        +(communicator == null) + ", Connected ?  "
                        +(btConnectionState == IBluetoothCommunicator.CONNECTED)
                , DebugMessage.TYPE_BLUETOOTH);

        if (communicator == null
                || btConnectionState != IBluetoothCommunicator.CONNECTED) {
            return;
        }

        if (payload == null || payload.isEmpty()) {
            return;
        }

        try { // get instruction string from json payload
            String temp = new JSONObject(payload).getString("instruction");
            payload = temp;
        } catch (JSONException e) {
        }

        ArrayList<String> sendData = new ArrayList<>(payload.length() % 20 + 1);

        while (payload.length() > 20) {
            sendData.add(payload.substring(0, 20));
            payload = payload.substring(20);
        }
        sendData.add(payload);

        for (String data : sendData) {
            byte[] bytes;

            bytes = deviceInterface.getBytes(data);


            if (bytes == null || bytes.length == 0) {
                return;
            }

            communicator.writeData(bytes);
        }
    }

    public void connectionStateChange(int state) {
        Logger.getInstance().logV(TAG,"connectionStateChange() state:"+state
                , DebugMessage.TYPE_BLUETOOTH);
        btConnectionState = state;
        dataListener.getBluetoothState(state);

        // on device connected?
    }

    public void closeDeviceConnection(){
        Logger.getInstance().logV(TAG,"closeDeviceConnection()"
                , DebugMessage.TYPE_BLUETOOTH);
        if (communicator != null){
            communicator.close();
        }
        btConnectionState = IBluetoothCommunicator.DISCONNECTED;
    }

    /**
     * @param device
     */
    @SuppressLint("NewApi")
    public synchronized void connectToDevice(final BluetoothDevice device) {
        Logger.getInstance().logV(TAG,"connectToDevice() device: "+device
                , DebugMessage.TYPE_BLUETOOTH);

        if (btConnectionState == BluetoothCommunicator.CONNECTING) {
            Logger.getInstance().logI(TAG,"Connecting to device: Error, already connecting/connected to a device"
                    , DebugMessage.TYPE_BLUETOOTH);
            return;
        } else if (communicator != null && btConnectionState == BluetoothCommunicator.CONNECTED){
            communicator.close();
        }

        switch (deviceInterface.commType()) {
            case LE:
                btConnectionState = BluetoothCommunicator.CONNECTING;
                dataListener.getBluetoothState(btConnectionState);
                Log.i(TAG, "Connecting to LE device");
                if (communicator == null || !(communicator instanceof BluetoothLeComm)){
                    communicator = new BluetoothLeComm(mContext, this);
                }
                ((BluetoothLeComm) communicator)
                        .setReadChar(deviceInterface.getReadChar());
                ((BluetoothLeComm) communicator)
                        .setServiceUuid(deviceInterface.getServiceUuid());
                ((BluetoothLeComm) communicator)
                        .setWriteChar(deviceInterface.getWriteChar());
                break;
            case CLASSIC:
                btConnectionState = BluetoothCommunicator.CONNECTING;
                dataListener.getBluetoothState(btConnectionState);
                Log.i(TAG, "Connecting to Classic device");
                if (communicator == null){
                    communicator = new BluetoothClassicComm(mContext, this);
                }
                break;
        }

        Logger.getInstance().logI(TAG,"Connecting to device: deviceName="+device.getName()
                , DebugMessage.TYPE_BLUETOOTH);
        communicator.connectToDevice(device);
    }

    public void bluetoothStateChanged(int state) {
        Logger.getInstance().logV(TAG,"bluetoothStateChanged() state: "+state
                , DebugMessage.TYPE_BLUETOOTH);

        if (state == BluetoothAdapter.STATE_OFF) {
            btConnectionState = BluetoothCommunicator.DISCONNECTED;
            dataListener.getBluetoothState(btConnectionState);
            if (communicator != null){
                communicator.bluetoothStateChanged(state);
            }
        }
    }

    public int getState() {
        return btConnectionState;
    }

    private synchronized boolean connectBluetooth(boolean urgent) {
        Logger.getInstance().logV(TAG,"connectBluetooth() urgent: "+urgent
                , DebugMessage.TYPE_BLUETOOTH);
        nonUrgentScanInProgress = !urgent; //Set the flag regardless of whether a scan is in progress
        btConnectionState = communicator == null ? BluetoothCommunicator.DISCONNECTED : communicator.getState();

        if (btConnectionState == BluetoothCommunicator.CONNECTED) {
            Log.i(TAG, "Bluetooth connected");
            return false;
        }

        if (btConnectionState == BluetoothCommunicator.CONNECTING) {
            Log.i(TAG, "Bluetooth already connecting");
            return false;
        }

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Log.i(TAG, "Bluetooth not enabled or BluetoothAdapt is null");
            return false;
        }

        //Order matters in the IF condition below, if rssiScan=true then discovery will not be started
        if (!rssiScan && mBluetoothAdapter.startDiscovery()){
            Logger.getInstance().logI(TAG,"Discovery started", DebugMessage.TYPE_BLUETOOTH);
            //If discovery takes longer than 20 seconds, timeout and cancel it
            discoveryWasStarted = true;
            discoveryNum++;
            useCaseComponent.discoveryTimeoutUseCase().execute(discoveryNum, timerDiscoveryNum -> {
                if (discoveryNum == timerDiscoveryNum
                        && discoveryWasStarted){
                    Logger.getInstance().logE(TAG,"Discovery timeout", DebugMessage.TYPE_BLUETOOTH);
                    mBluetoothAdapter.cancelDiscovery();
                }
            });

            rssiScan = true;
            foundDevices.clear(); //Reset found devices map from previous scan
            return true;
        }
        else{
            return false;
        }
    }

    private void connectTo212Device(BluetoothDevice device){
        Logger.getInstance().logV(TAG,"connectTo212Device() "+device
                , DebugMessage.TYPE_BLUETOOTH);
        deviceInterface = new Device212B(mContext, dataListener
                , BluetoothDeviceManager.this, device.getName());
        connectToDevice(device);
    }



    private void connectTo215Device(BluetoothDevice device) {
        Logger.getInstance().logV(TAG,"connectTo215Device() device: "+device
                , DebugMessage.TYPE_BLUETOOTH);
        useCaseComponent.getPrevIgnitionTimeUseCase().execute(device.getName()
                , new GetPrevIgnitionTimeUseCase.Callback() {

                    @Override
                    public void onGotIgnitionTime(long ignitionTime) {
                        Log.v(TAG, "Received ignition time: " + ignitionTime);
                        deviceInterface = new Device215B(mContext, dataListener
                                , device.getName(), ignitionTime);
                        connectToDevice(device);

                    }

                    @Override
                    public void onNoneExists() {
                        Log.v(TAG, "No previous ignition time exists!");
                        deviceInterface = new Device215B(mContext, dataListener
                                , device.getName());
                        connectToDevice(device);
                    }

                    @Override
                    public void onError(RequestError error) {
                        deviceInterface = new Device215B(mContext, dataListener
                                , device.getName());
                        connectToDevice(device);
                        Log.v(TAG, "ERROR: could not get previous ignition time");

                    }
                });
    }

    public boolean moreDevicesLeft(){
        return foundDevices.size() > 0;
    }

    //Returns whether a device qualified for connection
    public synchronized boolean connectToNextDevice(){
        Logger.getInstance().logV(TAG,"connectToNextDevice(), foundDevices count: "+foundDevices.keySet().size()
                , DebugMessage.TYPE_BLUETOOTH);
        short minRssiThreshold;
        short strongestRssi = Short.MIN_VALUE;
        BluetoothDevice strongestRssiDevice = null;

        if (nonUrgentScanInProgress){
            minRssiThreshold = -70;

        }
        //Deliberate scan, connect regardless
        else{
            minRssiThreshold = Short.MIN_VALUE;
        }

        Log.d(TAG,"minRssiThreshold set to: "+minRssiThreshold);

        for (Map.Entry<BluetoothDevice,Short> device: foundDevices.entrySet()){
            if (device.getValue() != null && device.getValue() > strongestRssi){
                strongestRssiDevice = device.getKey();
                strongestRssi = device.getValue();
            }
        }

        if (strongestRssiDevice == null || strongestRssi < minRssiThreshold) {
            Log.d(TAG,"No device was found as candidate for a potential connection.");
            foundDevices.clear();
            return false;

        }

        if (strongestRssiDevice.getName().contains(ObdManager.BT_DEVICE_NAME_212)) {
            Log.d(TAG, "device212 > RSSI_Threshold, device: " + strongestRssiDevice);

            foundDevices.remove(strongestRssiDevice);
            connectTo212Device(strongestRssiDevice);
        } else if (strongestRssiDevice.getName().contains(ObdManager.BT_DEVICE_NAME_215)) {
            Log.d(TAG, "device215 > RSSI_Threshold, device: " + strongestRssiDevice);

            foundDevices.remove(strongestRssiDevice);
            connectTo215Device(strongestRssiDevice);
        }
        return true;
    }

    private Map<BluetoothDevice, Short> foundDevices = new HashMap<>(); //Devices found by receiver
    private boolean rssiScan = false;

    // for classic discovery
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            //Found device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);

                Log.d(TAG, "name: "+device.getName() + ", address: " + device.getAddress()+" RSSI: "+rssi);

                //Store all devices in a map
                if (device.getName() != null && device.getName().contains(ObdManager.BT_DEVICE_NAME)
                        && !foundDevices.containsKey(device)){
                    foundDevices.put(device,rssi);
                    Log.d(TAG,"foundDevices.put() device name: "+device.getName());
                }
                else{
                    Log.d(TAG,"Device did not meet criteria for foundDevice list");
                }
            }
            //Finished scanning
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){

                discoveryWasStarted = false;
                discoveryNum++;
                Logger.getInstance().logI(TAG,"Discovery finished", DebugMessage.TYPE_BLUETOOTH);

                //Connect to device with strongest signal if scan has been requested
                if (rssiScan){
                    rssiScan = false;
                    dataListener.onDevicesFound();
                    String foundDevicesString = "{";
                    for (Map.Entry<BluetoothDevice,Short> d: foundDevices.entrySet()){
                        foundDevicesString += d.getKey().getName()+"="+d.getValue()+",";
                    }
                    foundDevicesString+="}";

                    Logger.getInstance().logI(TAG,"Found devices: "+foundDevicesString, DebugMessage.TYPE_BLUETOOTH);
                    mixpanelHelper.trackFoundDevices(foundDevices);
                    if (foundDevices.size() > 0){
                        //Try to connect to available device, if none qualify then finish scan
                        if (!connectToNextDevice()) dataListener.scanFinished();
                    }else{
                        //Notify scan finished after 0.5 seconds due to delay
                        // in receiving CONNECTING notification
                        dataListener.scanFinished();
                    }
                }

            }
        }
    };

    public void readData(final byte[] data) {
        mHandler.post(() -> deviceInterface.parseData(data));
    }

    // functions

    public void getVin() {
        Log.d(TAG,"getVin()");
        if (btConnectionState != BluetoothCommunicator.CONNECTED) {
            return;
        }

        writeToObd(deviceInterface.getVin()); // 212 parser returns json
    }

    public void getRtc() {
        Log.d(TAG,"getRtc()");
        if (btConnectionState != BluetoothCommunicator.CONNECTED) {
            return;
        }

        writeToObd(deviceInterface.getRtc());
    }

    public void setDeviceNameAndId(String id){
        Log.d(TAG,"setDeviceNameAndId() id: "+id);
        //Device name should never be set for 212
        if (isConnectedTo215()){
            Device215B device215B = (Device215B)deviceInterface;
            writeToObd(device215B.setDeviceNameAndId(ObdManager.BT_DEVICE_NAME_215 + " " + id,id));
        }
    }

    public void setDeviceId(String id){
        Log.d(TAG,"setDeviceId() id: "+id);
        if (isConnectedTo215()){
            Device215B device215B = (Device215B)deviceInterface;
            Log.d(TAG,"Setting device id to "+id+", command: "+device215B.setDeviceId(id));
            writeToObd(device215B.setDeviceId(id));
        }
    }

    public void clearDeviceMemory(){
        Log.d(TAG, "clearDeviceMemory() ");
        if (isConnectedTo215()){
            Log.d(TAG, "clearing Device Memory, command:  " + deviceInterface.clearDeviceMemory());
            writeToObd(deviceInterface.clearDeviceMemory());
        }
    }

    public void resetDeviceToDefaults(){

        Log.d(TAG, "resetToDefualts() ");
        if (isConnectedTo215()){
            Log.d(TAG, "resetting to defaults, command:  " + deviceInterface.resetDeviceToDefaults());
            writeToObd(deviceInterface.resetDeviceToDefaults());
        }

    }

    public void resetDevice(){
        Log.d(TAG, "resetDevice() ");
        if (isConnectedTo215()){
            Log.d(TAG, "resetting Device, command:  " + deviceInterface.resetDevice());
            writeToObd(deviceInterface.resetDevice());
        }

    }
    public void setRtc(long rtcTime) {
        Log.d(TAG,"setRtc() rtc: "+rtcTime);
        if (btConnectionState != BluetoothCommunicator.CONNECTED) {
            return;
        }

        writeToObd(deviceInterface.setRtc(rtcTime));
    }

    public void getPids(String pids) {
        Log.d(TAG,"getPids() pids: "+pids);
        if (btConnectionState != BluetoothCommunicator.CONNECTED) {
            return;
        }

        writeToObd(deviceInterface.getPids(pids));
    }

    public void clearDtcs(){
        Log.d(TAG, "clearDTCs");
        if (isConnectedTo215()){
            Log.d(TAG, "clearing DTC, command:  " + deviceInterface.clearDtcs());
            writeToObd(deviceInterface.clearDtcs());
        }
    }
    public void getSupportedPids() {
        Logger.getInstance().logI(TAG,"Requested supported pid", DebugMessage.TYPE_BLUETOOTH);
        if (btConnectionState != BluetoothCommunicator.CONNECTED) {
            return;
        }

        writeToObd(deviceInterface.getSupportedPids());
    }

    // sets pids to check and sets data interval
    public void setPidsToSend(String pids, int timeInterval) {
        Logger.getInstance().logI(TAG,"Set pids to be sent: "+pids+", interval: "+timeInterval, DebugMessage.TYPE_BLUETOOTH);
        if (btConnectionState != BluetoothCommunicator.CONNECTED) {
            return;
        }

        writeToObd(deviceInterface.setPidsToSend(pids,timeInterval));
    }

    public void getDtcs() {
        Log.d(TAG,"getDtcs()");
        if (btConnectionState != BluetoothCommunicator.CONNECTED) {
            return;
        }

        writeToObd(deviceInterface.getDtcs());
        writeToObd(deviceInterface.getPendingDtcs());
    }

    public void getFreezeFrame() {
        Log.d(TAG,"getFreezeFrame()");
        if (btConnectionState != BluetoothCommunicator.CONNECTED) {
            return;
        }

        writeToObd(deviceInterface.getFreezeFrame());
    }

    public void requestData() {
        Log.d(TAG,"requestData()");
        if (btConnectionState != BluetoothCommunicator.CONNECTED) {
            return;
        }

        writeToObd(deviceInterface.requestData());
    }

    public void requestSnapshot(){
        Log.d(TAG,"requestSnapshot()");
        if (deviceInterface instanceof Device215B
                && btConnectionState == BluetoothCommunicator.CONNECTED){
            Log.d(TAG,"executing writeToOBD requestSnapshot()");
            writeToObd(deviceInterface.requestSnapshot());
        }
    }

    public boolean isConnectedTo215(){
        Log.d(TAG,"isConnectedTo215()");
        if (deviceInterface != null)
            return deviceInterface instanceof Device215B;
        else
            return false;
    }

    public int getConnectionState(){
        Log.d(TAG,"getConnectionState()");
        return btConnectionState;
    }

}
