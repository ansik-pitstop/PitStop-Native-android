package com.pitstop.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.util.Log;

import com.continental.rvd.mobile_sdk.SDKIntentService;
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
import com.pitstop.bluetooth.searcher.RVDBLuetoothDeviceSearcherStatusListener;
import com.pitstop.bluetooth.searcher.RVDBluetoothDeviceSearcher;
import com.pitstop.bluetooth.searcher.RegularBluetoothDeviceSearcher;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.models.DebugMessage;
import com.pitstop.utils.Logger;

import org.jetbrains.annotations.NotNull;

/**
 * Created by Ben!
 */
public class BluetoothDeviceManager implements RVDBLuetoothDeviceSearcherStatusListener{

    private static final String TAG = BluetoothDeviceManager.class.getSimpleName();

    private Context mContext;
    private GlobalApplication application;
    private ObdManager.IBluetoothDataListener dataListener;
    private RegularBluetoothDeviceSearcher regularBluetoothDeviceSearcher;
    private RVDBluetoothDeviceSearcher rvdBluetoothDeviceSearcher;

    private AbstractDevice deviceInterface;
    private UseCaseComponent useCaseComponent;

    private int btConnectionState = BluetoothCommunicator.DISCONNECTED;

    public enum CommType {
        CLASSIC, LE
    }

    public enum DeviceType {
        ELM327, OBD215, OBD212
    }

    public BluetoothDeviceManager(Context context, SDKIntentService sdkIntentService) {

        mContext = context;
        application = (GlobalApplication) context.getApplicationContext();

        useCaseComponent = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(mContext))
                .build();

        regularBluetoothDeviceSearcher
                = new RegularBluetoothDeviceSearcher(useCaseComponent,dataListener,context,this);
        rvdBluetoothDeviceSearcher = new RVDBluetoothDeviceSearcher(sdkIntentService,this);
    }

    /*
     * Callback methods for RVD SDK device searcher interface
     */


    @Override
    public void onBindingRequired() {
        Log.d(TAG,"onBindingRequired()");
    }

    @Override
    public void onBindingQuestionPrompted(@NotNull String question) {
        Log.d(TAG,"onBindingQuestionPrompted() question: "+question);

    }

    @Override
    public void onFirmwareUpdateRequired() {
        Log.d(TAG,"onFirmwareUpdateRequired()");
    }

    @Override
    public void onFirmwareUpdateStatus(int status) {
        Log.d(TAG,"onFirmwareUpdateStatus() status:"+status);
    }

    @Override
    public void onError(@NotNull String err) {
        Log.d(TAG,"onError() err: "+err);
    }

    @Override
    public void onConnectionCompleted(@NotNull AbstractDevice device) {

    }

    /*
     * End of callback methods for RVD SDK
     */

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

    public void setBluetoothDataListener(ObdManager.IBluetoothDataListener dataListener) {
        this.dataListener = dataListener;
    }

    public synchronized boolean startScan(boolean urgent, boolean ignoreVerification) {
        Log.d(TAG, "startScan() urgent: " + Boolean.toString(urgent) + " ignoreVerification: " + Boolean.toString(ignoreVerification));
        return regularBluetoothDeviceSearcher.startScan(urgent,ignoreVerification);
    }

    public synchronized void onConnectDeviceValid(){
        regularBluetoothDeviceSearcher.onConnectDeviceValid();
    }

    public void close() {
        Log.d(TAG,"close()");
        regularBluetoothDeviceSearcher.close();
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
            if (deviceInterface != null){
                deviceInterface.setCommunicatorState(state);
            }
        }
    }

    public void changeScanUrgency(boolean urgent){
        regularBluetoothDeviceSearcher.changeScanUrgency(urgent);
    }

    public void getVin() {
        Log.d(TAG,"getVin() btConnectionState: "+btConnectionState);
        if (btConnectionState != BluetoothCommunicator.CONNECTED) {
            return;
        }
        Log.d(TAG, "deviceInterface.getVin()");
        boolean ret = deviceInterface.getVin();
        Log.d(TAG,"get vin returned "+ret);
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
