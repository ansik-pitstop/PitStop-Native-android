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
import com.pitstop.models.DebugMessage;
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
    private ObdManager.IBluetoothDataListener dataListener; //Listener to bluetooth data, in this case BluetoothService

    private BluetoothAdapter mBluetoothAdapter; //Android class used to start discovery process
    private Handler mHandler = new Handler();

    private AbstractDevice deviceInterface; //Device that we are interfacing with(ELM, OBD215, OBD212)
    private UseCaseComponent useCaseComponent;

    private int discoveryNum = 0;   //Discovery counter used to figure out whether a particular discovery has timed out
    private int btConnectionState = BluetoothCommunicator.DISCONNECTED; //Current bluetooth connection state (This class doesn't distinguish between verified or non-verified connections)
    private boolean nonUrgentScanInProgress = false;    //Whether the bluetooth signal strength should make a difference in making a connection
    private boolean discoveryWasStarted = false;        //Prevent discovery from being started multiple times

    public void setState(int state) {

        Log.d(TAG,"setState() state: "+state);
        this.btConnectionState = state;
        dataListener.getBluetoothState(state);

    }

    public void onGotVin(String VIN, String deviceID) {
        Log.d(TAG, "onGotVin: "+VIN);
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

    /**
     * Starts the search for bluetooth devices. If a search is already in progress
     * or for some other reason the bluetooth adapter responds that it is already discovering when
     * it really isn't(this is a known bug), then no search will be started.
     * @param urgent whether how close the bluetooth device is should make an impact of the connection
     * @param ignoreVerification whether VIN and device id should not be verified upon connecting
     * @return true if the bluetooth search has started, and false otherwise
     */
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

    /**
     * Invoked if the device that has just been connected to is deemed valid,
     * and the rest of the devices found during the discovery process should
     * be discarded
     */
    public synchronized void onConnectDeviceValid(){
        if (mBluetoothAdapter.isEnabled() && mBluetoothAdapter.isDiscovering()){
            Log.i(TAG,"Stopping scan");
            mBluetoothAdapter.cancelDiscovery();
            dataListener.scanFinished();
        }
    }

    /**
     * Closes all bluetooth device connections, cancels any bluetooth discovery,
     * and unregisters bluetooth receivers. Sets the state to disconnected.
     */
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

    /**
     * Close connection with currently connected to device
     */
    public void closeDeviceConnection(){
        Log.d(TAG,"closeDeviceConnection()");
        if (deviceInterface != null){
            deviceInterface.closeConnection();
        }
        btConnectionState = IBluetoothCommunicator.DISCONNECTED;
    }

    /**
     * Change bluetooth state
     * @param state bluetooth state to change to
     */
    public void bluetoothStateChanged(int state) {
        if (state == BluetoothAdapter.STATE_OFF) {
            btConnectionState = BluetoothCommunicator.DISCONNECTED;
            dataListener.getBluetoothState(btConnectionState);
            if (deviceInterface != null){
                deviceInterface.setCommunicatorState(state);
            }
        }
    }

    /**
     * Change urgency of current or future scan
     * @param urgent whether the scan is urgent and should use bluetooth proximity in deciding connection
     */
    public void changeScanUrgency(boolean urgent){
        this.nonUrgentScanInProgress = !urgent;
    }

    /**
     *
     * @return current bluetooth state
     */
    public int getState() {
        return btConnectionState;
    }

    /**
     * Start the discovery process if possible and add found devices to list
     * @param urgent
     * @return whether the discovery process has been started successfully
     */
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

    private void connectTo215Device(BluetoothDevice device) {
        Log.d(TAG,"connectTo215Device() device: "+device.getName());
        deviceInterface = new Device215B(mContext, dataListener
                , device.getName(), BluetoothDeviceManager.this);

        deviceInterface.connectToDevice(device);

    }

    private void connectToELMDevice(BluetoothDevice device){
        Log.d(TAG,"connectToELM327Device() device: "+device.getName());
        deviceInterface = new ELM327Device( mContext, this);
        dataListener.setDeviceName(device.getAddress());
        deviceInterface.connectToDevice(device);
    }

    /**
     *
     * @return true if more devices are available for potential connection
     */
    public boolean moreDevicesLeft(){
        return foundDevices.size() > 0;
    }

    /**
     * Connects to a device from the device list formed during the discovery process.
     * Urgent flag dictates the minimum bluetooth signal requirements. The device
     * which is identified to be supported by the Pitstop App with the highest signal
     * will be the one connected to.
     * @return whether any device qualified for connection
     */
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
        Log.d(TAG,"strongest rssi: "+strongestRssi);
        if (strongestRssiDevice == null || strongestRssi < minRssiThreshold) {
            Log.d(TAG,"No device was found as candidate for a potential connection.");
            foundDevices.clear();
            return false;

        }

        //Close previous connection
        if (deviceInterface != null){
            deviceInterface.closeConnection();
        }

        if (strongestRssiDevice.getName().contains(ObdManager.BT_DEVICE_NAME_215)) {
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
    private boolean rssiScan = false;   //whether the rssi scan can be started

    /**
     * Used for classic discovery, handles adding all supported found devices to
     * the device list and then when finished handles connecting
     */
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
                    Log.d(TAG,"Device did not meet criteria for foundDevice list name="+device.getName());
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

    /**
     * Request VIN from the device
     */
    public void getVin() {
        Log.d(TAG,"getVin() btConnectionState: "+btConnectionState);
        if (btConnectionState != BluetoothCommunicator.CONNECTED) {
            return;
        }
        Log.d(TAG, "deviceInterface.getVin()");
        boolean ret = deviceInterface.getVin();
        Log.d(TAG,"get vin returned "+ret);
    }

    /**
     * Request rtc time from the device
     */
    public void getRtc() {
        Log.d(TAG,"getRtc()");
        if (btConnectionState != BluetoothCommunicator.CONNECTED) {
            return;
        }
        deviceInterface.getRtc();
        /*writeToObd(deviceInterface.getRtc());*/
    }

    /**
     * Set the device name and id of the device
     *
     * @param id device name and id to be set
     */
    public void setDeviceNameAndId(String id){
        Log.d(TAG,"setDeviceNameAndId() id: "+id);
        //Device name should never be set for 212
        if (deviceInterface instanceof Device215B){
            Device215B device215B = (Device215B)deviceInterface;
            device215B.setDeviceNameAndId(ObdManager.BT_DEVICE_NAME_215 + " " + id,id);
        }
    }

    /**
     * Set the device id
     *
     * @param id device id to be set
     */
    public void setDeviceId(String id){
        Log.d(TAG,"setDeviceId() id: "+id);
        if (deviceInterface instanceof Device215B){
            Device215B device215B = (Device215B)deviceInterface;
            device215B.setDeviceId(id);
        }
    }

    /**
     * Clear memory of the device
     */
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

    /**
     * Set rtc time on the device
     *
     * @param rtcTime time on the device to be set
     */
    public void setRtc(long rtcTime) {
        Log.d(TAG,"setRtc() rtc: "+rtcTime);
        if (btConnectionState != BluetoothCommunicator.CONNECTED) {
            return;
        }
        deviceInterface.setRtc(rtcTime);
    }

    /**
     * Request a specific list of pids from the device for return once
     *
     * @param pids pids to be returned once from the device
     */
    public void getPids(String pids) {
        Log.d(TAG,"getPids() pids: "+pids);
        if (btConnectionState != BluetoothCommunicator.CONNECTED) {
            return;
        }
        deviceInterface.getPids(pids);
    }

    /**
     * Clear all the stored DTC from the vehicle through the device
     */
    public void clearDtcs(){
        Log.d(TAG, "clearDTCs");
        if (deviceInterface instanceof Device215B ||
                deviceInterface instanceof ELM327Device){
            deviceInterface.clearDtcs();
        }
    }

    /**
     * Get list of pids that can be returned from this vehicle through the device
     */
    public void getSupportedPids() {
        Logger.getInstance().logI(TAG,"Requested supported pid", DebugMessage.TYPE_BLUETOOTH);
        if (btConnectionState != BluetoothCommunicator.CONNECTED) {
            return;
        }
        deviceInterface.getSupportedPids();
    }

    /**
     * Set list of pids to returned periodically from the device
     *
     * @param pids pids to be returned periodically (up to 10 for OBD2 devices)
     * @param timeInterval how often they are to be returned
     */
    public void setPidsToSend(String pids, int timeInterval) {
        Logger.getInstance().logI(TAG,"Set pids to be sent: "+pids+", interval: "+timeInterval, DebugMessage.TYPE_BLUETOOTH);
        if (btConnectionState != BluetoothCommunicator.CONNECTED) {
            return;
        }
        deviceInterface.setPidsToSend(pids,timeInterval);
    }

    /**
     * Request both stored and pending DTCs from the device
     */
    public void getDtcs() {
        Log.d(TAG,"getDtcs()");
        if (btConnectionState != BluetoothCommunicator.CONNECTED) {
            Log.d(TAG," can't get Dtcs because my sate is not connected");
            return;
        }
        deviceInterface.getDtcs();
        deviceInterface.getPendingDtcs();
    }

    /**
     * Get freeze frame from the device
     */
    public void getFreezeFrame() {
        Log.d(TAG,"getFreezeFrame()");
        if (btConnectionState != BluetoothCommunicator.CONNECTED) {
            return;
        }

        deviceInterface.getFreezeFrame();
    }

    /**
     * Request IDR data from device
     */
    public void requestData() {
        Log.d(TAG,"requestData()");
        if (btConnectionState != BluetoothCommunicator.CONNECTED) {
            return;
        }

        deviceInterface.requestData();
    }

    /**
     * Request pid snapshot from the device
     */
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

    /**
     *
     * @return type of device that app is currently connected to
     */
    public DeviceType getDeviceType(){
        Log.d(TAG,"isConnectedTo215()");
        if (deviceInterface != null)
            if (deviceInterface instanceof Device215B){
                return DeviceType.OBD215;
            }else if (deviceInterface instanceof ELM327Device){
                return DeviceType.ELM327;
            }else{
                return null;
            }
        else
            return null;
    }

    /**
     * Request the description of the protocol currently being used by ELM327 device
     *
     * @return true if connection to ELM327 device is present, and false otherwise
     */
    public boolean requestDescribeProtocol(){
        Log.d(TAG,"requestDescribeProtocol");
        if (deviceInterface != null && deviceInterface instanceof ELM327Device){
            ((ELM327Device)deviceInterface).requestDescribeProtocol();
            return true;
        }else{
            return false;
        }
    }

    /**
     * Request 2141 emissions pid
     *
     * @return true is connection is present with appropriate device, and false otherwise
     */
    public boolean request2141PID(){
        Log.d(TAG,"requestDescribeProtocol");
        if (deviceInterface != null && deviceInterface instanceof ELM327Device){
            ((ELM327Device)deviceInterface).request2141PID();
            return true;
        }else{
            return false;
        }
    }

    /**
     * Request all stored DTC in vehicle through device
     *
     * @return true if currently connected to device is ELM327, and false otherwise
     */
    public boolean requestStoredDTC(){
        Log.d(TAG,"requestDescribeProtocol");
        if (deviceInterface != null && deviceInterface instanceof ELM327Device){
            ((ELM327Device)deviceInterface).requestStoredTroubleCodes();
            return true;
        }else{
            return false;
        }
    }

    /**
     * Request all pending DTC in vehicle through device
     *
     * @return true if currently connected to device is ELM327, and false otherwise
     */
    public boolean requestPendingDTC(){
        Log.d(TAG,"requestDescribeProtocol");
        if (deviceInterface != null && deviceInterface instanceof ELM327Device){
            ((ELM327Device)deviceInterface).requestPendingTroubleCodes();
            return true;
        }else{
            return false;
        }
    }

    /**
     * Select protocol to be used to communicate with the vehicle through the ELM327 device
     *
     * @return true if currently connected to device is ELM327, and false otherwise
     */
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
