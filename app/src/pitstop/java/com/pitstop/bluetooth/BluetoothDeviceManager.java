package com.pitstop.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.util.Log;

import com.pitstop.application.GlobalApplication;
import com.pitstop.bluetooth.bleDevice.AbstractDevice;
import com.pitstop.bluetooth.bleDevice.Device212B;
import com.pitstop.bluetooth.bleDevice.Device215B;
import com.pitstop.bluetooth.bleDevice.ELM327Device;
import com.pitstop.bluetooth.communicator.BluetoothCommunicator;
import com.pitstop.bluetooth.communicator.IBluetoothCommunicator;
import com.pitstop.bluetooth.communicator.ObdManager;
import com.pitstop.bluetooth.dataPackages.DtcPackage;
import com.pitstop.bluetooth.dataPackages.PidPackage;
import com.pitstop.bluetooth.elm.enums.ObdProtocols;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.get.GetPrevIgnitionTimeUseCase;
import com.pitstop.models.DebugMessage;
import com.pitstop.network.RequestError;
import com.pitstop.utils.Logger;
import com.pitstop.utils.MixpanelHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Ben!
 */
public class BluetoothDeviceManager{

    private static final String TAG = BluetoothDeviceManager.class.getSimpleName();

    private Context mContext;
    private GlobalApplication application;
    private MixpanelHelper mixpanelHelper;
    private ObdManager.IBluetoothDataListener dataListener;

    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler = new Handler();

    private AbstractDevice deviceInterface;
    private UseCaseComponent useCaseComponent;

    private int discoveryNum = 0;
    private int btConnectionState = BluetoothCommunicator.DISCONNECTED;
    private boolean nonUrgentScanInProgress = false;
    private boolean discoveryWasStarted = false;

    public void setState(int state) {
        Log.d(TAG,"setState() state: "+state);
        this.btConnectionState = state;
        dataListener.getBluetoothState(state);

    }

    public void onGotVin(String VIN, String deviceID) {
        Log.d(TAG, VIN);
        dataListener.handleVinData(VIN, deviceID);
    }

    public void gotDtcData(DtcPackage dtcPackage) {
        Log.d(TAG, "gotDtcData");
        dataListener.dtcData(dtcPackage);
    }

    public void gotPidPackage(PidPackage pidPackage) {
        Log.d(TAG, "pidData: " + pidPackage.toString());
        dataListener.idrPidData(pidPackage);
        dataListener.pidData(pidPackage);
    }

    public void onGotRtc(long l) {

        dataListener.onGotRtc(l);
    }

    public enum CommType {
        CLASSIC, LE
    }

    public enum DeviceType {
        ELM327, OBD215, OBD212
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
        Log.d(TAG, "startScan() urgent: " + Boolean.toString(urgent) + " ignoreVerification: " + Boolean.toString(ignoreVerification));
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
        if (mBluetoothAdapter.isEnabled() && mBluetoothAdapter.isDiscovering()){
            Log.i(TAG,"Stopping scan");
            mBluetoothAdapter.cancelDiscovery();
            dataListener.scanFinished();
        }
    }

    public void close() {
        Log.d(TAG,"close()");
        btConnectionState = IBluetoothCommunicator.DISCONNECTED;
        if (mBluetoothAdapter != null && mBluetoothAdapter.isDiscovering()){
            mBluetoothAdapter.cancelDiscovery();
            dataListener.scanFinished();
        }

        if (deviceInterface != null ) {
            deviceInterface.closeConnection();
        }
        try {
            mContext.unregisterReceiver(receiver);
        } catch (Exception e) {
            Log.d(TAG, "Receiver not registered");
        }
    }


    public void closeDeviceConnection(){
        Log.d(TAG,"closeDeviceConnection()");
        if (deviceInterface != null){
            deviceInterface.closeConnection();
        }
        btConnectionState = IBluetoothCommunicator.DISCONNECTED;
    }

    public void bluetoothStateChanged(int state) {
        if (state == BluetoothAdapter.STATE_OFF) {
            btConnectionState = BluetoothCommunicator.DISCONNECTED;
            dataListener.getBluetoothState(btConnectionState);
            if (deviceInterface != null){
                deviceInterface.setCommunicatorState(state);
            }
        }
    }

    public int getState() {
        return btConnectionState;
    }

    private synchronized boolean connectBluetooth(boolean urgent) {
        nonUrgentScanInProgress = !urgent; //Set the flag regardless of whether a scan is in progress
        btConnectionState = deviceInterface == null ? BluetoothCommunicator.DISCONNECTED : deviceInterface.getCommunicatorState();

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
        Log.d(TAG,"connectTo212Device() device: "+device.getName());
        deviceInterface = new Device212B(mContext, dataListener, device.getName(), this);

        deviceInterface.connectToDevice(device);
    }

    private void connectTo215Device(BluetoothDevice device) {
        Log.d(TAG,"connectTo215Device() device: "+device.getName());
        useCaseComponent.getPrevIgnitionTimeUseCase().execute(device.getName()
                , new GetPrevIgnitionTimeUseCase.Callback() {

                    @Override
                    public void onGotIgnitionTime(long ignitionTime) {
                        Log.v(TAG, "Received ignition time: " + ignitionTime);
                        deviceInterface = new Device215B(mContext, dataListener
                                , device.getName(), ignitionTime, BluetoothDeviceManager.this);

                        deviceInterface.connectToDevice(device);

                    }

                    @Override
                    public void onNoneExists() {
                        Log.v(TAG, "No previous ignition time exists!");
                        deviceInterface = new Device215B(mContext, dataListener
                                , device.getName(), BluetoothDeviceManager.this);
                        deviceInterface.connectToDevice(device);
                    }

                    @Override
                    public void onError(RequestError error) {
                        deviceInterface = new Device215B(mContext, dataListener
                                , device.getName(), BluetoothDeviceManager.this);
                        deviceInterface.connectToDevice(device);
                        Log.v(TAG, "ERROR: could not get previous ignition time");

                    }
                });
    }

    private void connectToELMDevice(BluetoothDevice device){
        Log.d(TAG,"connectToELM327Device() device: "+device.getName());
        deviceInterface = new ELM327Device( mContext, this);
        dataListener.setDeviceName(device.getAddress());
        deviceInterface.connectToDevice(device);
    }

    public boolean moreDevicesLeft(){
        return foundDevices.size() > 0;
    }

    //Returns whether a device qualified for connection
    public synchronized boolean connectToNextDevice(){
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
            return false;

        }

        //Close previous connection
        if (deviceInterface != null){
            deviceInterface.closeConnection();
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
        else if (strongestRssiDevice.getName().contains(ObdManager.CARISTA_DEVICE)||
                strongestRssiDevice.getName().contains(ObdManager.VIECAR_DEVICE)||
                strongestRssiDevice.getName().contains(ObdManager.OBDII_DEVICE_NAME) ||
                strongestRssiDevice.getName().contains(ObdManager.OBD_LINK_MX)
                ){
            Log.d(TAG, "CaristaDevice> RSSI_Threshold, device: " + strongestRssiDevice);
            foundDevices.remove(strongestRssiDevice);
            connectToELMDevice(strongestRssiDevice);
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
                if (device.getName() != null && (device.getName().contains(ObdManager.BT_DEVICE_NAME) ||
                        device.getName().equalsIgnoreCase(ObdManager.CARISTA_DEVICE)
                        ||device.getName().equalsIgnoreCase(ObdManager.VIECAR_DEVICE)
                        || device.getName().equalsIgnoreCase(ObdManager.OBDII_DEVICE_NAME)
                        || device.getName().equalsIgnoreCase(ObdManager.OBD_LINK_MX))
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
    public void getVin() {
        Log.d(TAG,"getVin()");
        if (btConnectionState != BluetoothCommunicator.CONNECTED) {
            return;
        }
        Log.d(TAG, "deviceInterface.getVin()");
        deviceInterface.getVin();
    }

    public void getRtc() {
        Log.d(TAG,"getRtc()");
        if (btConnectionState != BluetoothCommunicator.CONNECTED) {
            return;
        }
        deviceInterface.getRtc();
        /*writeToObd(deviceInterface.getRtc());*/
    }

    public void setDeviceNameAndId(String id){
        Log.d(TAG,"setDeviceNameAndId() id: "+id);
        //Device name should never be set for 212
        if (deviceInterface instanceof Device215B){
            Device215B device215B = (Device215B)deviceInterface;
            device215B.setDeviceNameAndId(ObdManager.BT_DEVICE_NAME_215 + " " + id,id);
        }
    }

    public void setDeviceId(String id){
        Log.d(TAG,"setDeviceId() id: "+id);
        if (deviceInterface instanceof Device215B){
            Device215B device215B = (Device215B)deviceInterface;
            device215B.setDeviceId(id);
        }
    }



    public void clearDeviceMemory(){
        Log.d(TAG, "clearDeviceMemory() ");
        if (deviceInterface instanceof Device215B){
            ((Device215B)deviceInterface).clearDeviceMemory();

        }
    }

    public void resetDeviceToDefaults(){

        Log.d(TAG, "resetToDefualts() ");
        if (deviceInterface instanceof Device215B){
            ((Device215B)deviceInterface).resetDeviceToDefaults();
        }

    }

    public void resetDevice(){
        Log.d(TAG, "resetDevice() ");
        if (deviceInterface instanceof Device215B){
            ((Device215B)deviceInterface).resetDevice();
        }

    }
    public void setRtc(long rtcTime) {
        Log.d(TAG,"setRtc() rtc: "+rtcTime);
        if (btConnectionState != BluetoothCommunicator.CONNECTED) {
            return;
        }
        deviceInterface.setRtc(rtcTime);
    }

    public void getPids(String pids) {
        Log.d(TAG,"getPids() pids: "+pids);
        if (btConnectionState != BluetoothCommunicator.CONNECTED) {
            return;
        }
        deviceInterface.getPids(pids);
    }

    public void clearDtcs(){
        Log.d(TAG, "clearDTCs");
        if (deviceInterface instanceof Device215B ||
                deviceInterface instanceof ELM327Device){
            deviceInterface.clearDtcs();
        }
    }
    public void getSupportedPids() {
        Logger.getInstance().logI(TAG,"Requested supported pid", DebugMessage.TYPE_BLUETOOTH);
        if (btConnectionState != BluetoothCommunicator.CONNECTED) {
            return;
        }
        deviceInterface.getSupportedPids();
    }

    // sets pids to check and sets data interval
    public void setPidsToSend(String pids, int timeInterval) {
        Logger.getInstance().logI(TAG,"Set pids to be sent: "+pids+", interval: "+timeInterval, DebugMessage.TYPE_BLUETOOTH);
        if (btConnectionState != BluetoothCommunicator.CONNECTED) {
            return;
        }
        deviceInterface.setPidsToSend(pids,timeInterval);
    }

    public void getDtcs() {
        Log.d(TAG,"getDtcs()");
        if (btConnectionState != BluetoothCommunicator.CONNECTED) {
            Log.d(TAG," can't get Dtcs because my sate is not connected");
            return;
        }
        deviceInterface.getDtcs();
        deviceInterface.getPendingDtcs();
    }

    public void getFreezeFrame() {
        Log.d(TAG,"getFreezeFrame()");
        if (btConnectionState != BluetoothCommunicator.CONNECTED) {
            return;
        }

        deviceInterface.getFreezeFrame();
    }

    public void requestData() {
        Log.d(TAG,"requestData()");
        if (btConnectionState != BluetoothCommunicator.CONNECTED) {
            return;
        }

        deviceInterface.requestData();
    }

    public void requestSnapshot(){
        Log.d(TAG,"requestSnapshot()");
        if (deviceInterface!= null) {
            if ((deviceInterface instanceof Device215B || deviceInterface instanceof ELM327Device)
                    && btConnectionState == BluetoothCommunicator.CONNECTED) {
                Log.d(TAG, "executing writeToOBD requestSnapshot()");
                deviceInterface.requestSnapshot();
            }
        }
    }

    public DeviceType getDeviceType(){
        Log.d(TAG,"isConnectedTo215()");
        if (deviceInterface != null)
            if (deviceInterface instanceof Device215B){
                return DeviceType.OBD215;
            }else if (deviceInterface instanceof Device212B){
                return DeviceType.OBD212;
            }else if (deviceInterface instanceof ELM327Device){
                return DeviceType.ELM327;
            }else{
                return null;
            }
        else
            return null;
    }

    public boolean requestDescribeProtocol(){
        Log.d(TAG,"requestDescribeProtocol");
        if (deviceInterface != null && deviceInterface instanceof ELM327Device){
            ((ELM327Device)deviceInterface).requestDescribeProtocol();
            return true;
        }else{
            return false;
        }
    }

    public boolean request2141PID(){
        Log.d(TAG,"requestDescribeProtocol");
        if (deviceInterface != null && deviceInterface instanceof ELM327Device){
            ((ELM327Device)deviceInterface).request2141PID();
            return true;
        }else{
            return false;
        }
    }

    public boolean requestStoredDTC(){
        Log.d(TAG,"requestDescribeProtocol");
        if (deviceInterface != null && deviceInterface instanceof ELM327Device){
            ((ELM327Device)deviceInterface).requestStoredTroubleCodes();
            return true;
        }else{
            return false;
        }
    }

    public boolean requestPendingDTC(){
        Log.d(TAG,"requestDescribeProtocol");
        if (deviceInterface != null && deviceInterface instanceof ELM327Device){
            ((ELM327Device)deviceInterface).requestPendingTroubleCodes();
            return true;
        }else{
            return false;
        }
    }

    public boolean requestSelectProtocol(ObdProtocols p){
        Log.d(TAG,"requestDescribeProtocol");
        if (deviceInterface != null && deviceInterface instanceof ELM327Device){
            ((ELM327Device)deviceInterface).requestSelectProtocol(p);
            return true;
        }else{
            return false;
        }
    }

    public int getConnectionState(){
        Log.d(TAG,"getConnectionState()");
        return btConnectionState;
    }

}
