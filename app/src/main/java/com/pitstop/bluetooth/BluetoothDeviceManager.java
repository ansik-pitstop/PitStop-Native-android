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
import com.castel.obd.bluetooth.ObdManager;
import com.castel.obd215b.util.DataPackageUtil;
import com.pitstop.application.GlobalApplication;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.get.GetPrevIgnitionTimeUseCase;
import com.pitstop.interactors.other.DiscoveryTimeoutUseCase;
import com.pitstop.models.DebugMessage;
import com.pitstop.network.RequestError;
import com.pitstop.utils.LogUtils;
import com.pitstop.utils.MixpanelHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.facebook.FacebookSdk.getApplicationContext;

/**
 * Created by Ben!
 */
public class BluetoothDeviceManager implements ObdManager.IPassiveCommandListener {

    private Context mContext;
    private GlobalApplication application;
    private MixpanelHelper mixpanelHelper;
    private ObdManager.IBluetoothDataListener dataListener;

    private BluetoothAdapter mBluetoothAdapter;
    private static final long SCAN_PERIOD = 12000;
    private boolean hasDiscoveredServices = false;

    private Handler mHandler = new Handler();

    private static final String TAG = BluetoothDeviceManager.class.getSimpleName();

    private boolean needToScan = true; // need to scan after restarting bluetooth adapter even if mGatt != null

    public static final UUID OBD_IDD_212_MAIN_SERVICE =
            UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb"); // 212B
    public static final UUID OBD_READ_CHAR_212 =
            UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb"); // 212B
    public static final UUID OBD_WRITE_CHAR_212 =
            UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb"); // 212B

    public static final UUID OBD_IDD_215_MAIN_SERVICE =
            UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e"); // 215B
    public static final UUID OBD_READ_CHAR_215 =
            UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e"); // 215B
    public static final UUID OBD_WRITE_CHAR_215 =
            UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e"); // 215B

    public static final UUID CONFIG_DESCRIPTOR =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private int btConnectionState = BluetoothCommunicator.DISCONNECTED;

    private AbstractDevice deviceInterface;

    private BluetoothCommunicator communicator;

    private BluetoothDevice connectedDevice;

    private UseCaseComponent useCaseComponent;

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
        this.dataListener = dataListener;
    }

    //Returns false if search didn't begin again
    private boolean ignoreVerification = false;
    public synchronized boolean startScan(boolean urgent, boolean ignoreVerification) {
        this.ignoreVerification = ignoreVerification;
        if (!mBluetoothAdapter.isEnabled() && !urgent) {
            Log.i(TAG, "Scan unable to start");
            return false;
        }
        else if (!mBluetoothAdapter.isEnabled() && urgent){
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
        if (mBluetoothAdapter.isEnabled() && mBluetoothAdapter.isDiscovering()){
            Log.i(TAG,"Stopping scan");
            mBluetoothAdapter.cancelDiscovery();
            dataListener.scanFinished();
        }
    }

    @Override
    public void sendCommandPassive(String payload) {
        if (btConnectionState != BluetoothCommunicator.CONNECTED) {
            return;
        }

        writeToObd(payload);
    }

    public void close() {

        if (mBluetoothAdapter != null && mBluetoothAdapter.isDiscovering()){
            mBluetoothAdapter.cancelDiscovery();
            dataListener.scanFinished();
        }

        if (communicator != null) {
            communicator.close();
            communicator = null;
            connectedDevice = null;
        }
        try {
            mContext.unregisterReceiver(receiver);
        } catch (Exception e) {
            Log.d(TAG, "Receiver not registered");
        }
    }

    private void writeToObd(String payload) {
        if (communicator == null) {
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
        Log.d(TAG,"connectionStateChange() state:"+state);
        btConnectionState = state;
        dataListener.getBluetoothState(state);

        // on device connected?
    }

    //Disconnect from device, add it to invalid device list, reset scan
    public void onConnectedDeviceInvalid(){
        LogUtils.debugLogD(TAG, "Connected device recognized as invalid, disconnecting"
                , true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
        if (!moreDevicesLeft()){
            communicator.close();
            communicator = null;
            connectedDevice = null;
        }
        connectToNextDevice(); //Try to connect to next device retrieved during previous scan
    }

    /**
     * @param device
     */
    @SuppressLint("NewApi")
    public void connectToDevice(final BluetoothDevice device) {
        if (btConnectionState == BluetoothCommunicator.CONNECTING) {
            Log.d(TAG,"ConnectToDevice() device: "+device+", already connecting, return");
            return;
        }

        // Le can be used for 212 but it doesn't work properly on all versions of Android
        // scanLeDevice(false);// will stop after first device detection

        if (communicator != null && connectedDevice != null){
            communicator.close();
            communicator = null;
            connectedDevice = null;
        }

        switch (deviceInterface.commType()) {
            case LE:
                btConnectionState = BluetoothCommunicator.CONNECTING;
                dataListener.getBluetoothState(btConnectionState);
                Log.i(TAG, "Connecting to LE device");
                communicator = new BluetoothLeComm(mContext, this, deviceInterface.getServiceUuid(),
                        deviceInterface.getWriteChar(), deviceInterface.getReadChar());
                break;
            case CLASSIC:
                btConnectionState = BluetoothCommunicator.CONNECTING;
                dataListener.getBluetoothState(btConnectionState);
                Log.i(TAG, "Connecting to Classic device");
                communicator = new BluetoothClassicComm(mContext, this);
                break;
        }

        connectedDevice = device;
        communicator.connectToDevice(device);
    }

    public void bluetoothStateChanged(int state) {
        if (state == BluetoothAdapter.STATE_OFF) {
            btConnectionState = BluetoothCommunicator.DISCONNECTED;
            dataListener.getBluetoothState(btConnectionState);
        }
    }

    public int getState() {
        return btConnectionState;
    }

    public boolean isScanning(){
        return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()
            && mBluetoothAdapter.isDiscovering();
    }

    public String getConnectedDeviceName() {
        return deviceInterface.getDeviceName();
    }

    private boolean nonUrgentScanInProgress = false;
    private int discoveryNum = 0;
    private synchronized boolean connectBluetooth(boolean urgent) {
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

            //If discovery takes longer than 20 seconds, timeout and cancel it
            discoveryNum++;
            useCaseComponent.discoveryTimeoutUseCase().execute(discoveryNum, new DiscoveryTimeoutUseCase.Callback() {
                @Override
                public void onFinish(int timerDiscoveryNum) {
                    if (discoveryNum == timerDiscoveryNum){
                        Log.d(TAG,"discovery timeout!");
                        mBluetoothAdapter.cancelDiscovery();
                    }
                }
            });

            rssiScan = true;
            Log.i(TAG, "BluetoothAdapter starts discovery");
            foundDevices.clear(); //Reset found devices map from previous scan
            return true;
        }
        else{
            return false;
        }
    }

    private void connectTo212Device(BluetoothDevice device){
        Log.d(TAG,"connectTo212Device() device: "+device.getName());
        deviceInterface = new Device212B(mContext, dataListener
                , BluetoothDeviceManager.this, device.getName());
        connectToDevice(device);
    }

    private void connectTo215Device(BluetoothDevice device) {
        Log.d(TAG,"connectTo215Device() device: "+device.getName());
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

    private void connectToNextDevice(){
        Log.d(TAG,"connectToNextDevice(), foundDevices count: "+foundDevices.keySet().size());

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
            dataListener.scanFinished();
            return;

        }

        if (strongestRssiDevice.getName().contains(ObdManager.BT_DEVICE_NAME_212)) {
            Log.d(TAG, "device212 > RSSI_Threshold, device: "+strongestRssiDevice);

            foundDevices.remove(strongestRssiDevice);
            connectTo212Device(strongestRssiDevice);


        } else if (strongestRssiDevice.getName().contains(ObdManager.BT_DEVICE_NAME_215)) {

            Log.d(TAG, "device215 > RSSI_Threshold, device: " + strongestRssiDevice);

            foundDevices.remove(strongestRssiDevice);
            connectTo215Device(strongestRssiDevice);

        }
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
                    Log.d(TAG,"foundDevices.put() device name: "+device.getName());
                    foundDevices.put(device,rssi);
                }
                else{
                    Log.d(TAG,"Device did not meet criteria for foundDevice list");
                }
            }
            //Finished scanning
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){

                Log.d(TAG,"Discovery finished! rssi scan? "+rssiScan);
                //Connect to device with strongest signal if scan has been requested
                if (rssiScan){
                    rssiScan = false;
                    mixpanelHelper.trackFoundDevices(foundDevices);
                    Log.d(TAG,"mHandler().postDelayed() rssiScan, calling connectToNextDevce()");
                    connectToNextDevice();
                    if (!moreDevicesLeft()){

                        //Notify scan finished after 0.5 seconds due to delay
                        // in receiving CONNECTING notification
                        mHandler.postDelayed(() -> dataListener.scanFinished(),3000);
                    }
                }

            }
        }
    };

    public void readData(final byte[] data) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                deviceInterface.parseData(data);
            }
        });
    }

    // functions

    public void setParam(String param, String value) { // 215B specific
        if (btConnectionState != BluetoothCommunicator.CONNECTED) {
            return;
        }

        if (!(deviceInterface instanceof Device215B)) return;

        Log.d(TAG, DataPackageUtil.siMulti(param, value));
        writeToObd(DataPackageUtil.siMulti(param, value));
    }

    public void getVin() {
        if (btConnectionState != BluetoothCommunicator.CONNECTED) {
            return;
        }

        writeToObd(deviceInterface.getVin()); // 212 parser returns json
    }

    public void getRtc() {
        if (btConnectionState != BluetoothCommunicator.CONNECTED) {
            return;
        }

        writeToObd(deviceInterface.getRtc());
    }

    public void setDeviceNameAndId(String name){
        //Device name should never be set for 212
        if (isConnectedTo215()){
            Device215B device215B = (Device215B)deviceInterface;
            Log.d(TAG,"Setting device name and id to "+name+", command: "+device215B.setDeviceNameAndId(name));
            writeToObd(device215B.setDeviceNameAndId(name));
        }
    }

    public void setDeviceId(String id){
        if (isConnectedTo215()){
            Device215B device215B = (Device215B)deviceInterface;
            Log.d(TAG,"Setting device id to "+id+", command: "+device215B.setDeviceId(id));
            writeToObd(device215B.setDeviceId(id));
        }
    }

    public void setRtc(long rtcTime) {
        if (btConnectionState != BluetoothCommunicator.CONNECTED) {
            return;
        }

        writeToObd(deviceInterface.setRtc(rtcTime));
    }

    public void getPids(String pids) {
        if (btConnectionState != BluetoothCommunicator.CONNECTED) {
            return;
        }

        writeToObd(deviceInterface.getPids(pids));
    }

    public void getSupportedPids() {
        if (btConnectionState != BluetoothCommunicator.CONNECTED) {
            return;
        }

        writeToObd(deviceInterface.getSupportedPids());
    }

    // sets pids to check and sets data interval
    public void setPidsToSend(String pids) {
        Log.d(TAG,"setPidsToSend: "+pids);
        if (btConnectionState != BluetoothCommunicator.CONNECTED) {
            return;
        }

        writeToObd(deviceInterface.setPidsToSend(pids));
    }

    public void getDtcs() {
        if (btConnectionState != BluetoothCommunicator.CONNECTED) {
            return;
        }

        writeToObd(deviceInterface.getDtcs());
        writeToObd(deviceInterface.getPendingDtcs());
    }

    public void getFreezeFrame() {
        if (btConnectionState != BluetoothCommunicator.CONNECTED) {
            return;
        }

        writeToObd(deviceInterface.getFreezeFrame());
    }

    public void requestData() {
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

    public void getRtcAndMileage(){
        if (isConnectedTo215()){
            writeToObd(((Device215B) deviceInterface).getRtcAndMileage());
        }
    }

    public boolean isConnectedTo215(){
        if (deviceInterface != null)
            return deviceInterface instanceof Device215B;
        else
            return false;
    }

    public int getConnectionState(){
        return btConnectionState;
    }

}
