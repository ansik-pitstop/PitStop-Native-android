package com.pitstop.background;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.widget.Toast;

import com.castel.obd.bluetooth.BluetoothManage;
import com.castel.obd.info.DataPackageInfo;
import com.castel.obd.info.PIDInfo;
import com.castel.obd.info.ParameterPackageInfo;
import com.castel.obd.info.ResponsePackageInfo;
import com.pitstop.database.LocalDataRetriever;
import com.pitstop.database.models.Responses;

import java.util.HashMap;

/**
 * Created by David Liu on 11/30/2015.
 */
public class BluetoothAutoConnectService extends Service implements BluetoothManage.BluetoothDataListener{

    private final IBinder mBinder = new BluetoothBinder();
    private BluetoothManage.BluetoothDataListener serviceCallbacks;

    String[] pids = new String[0];
    int checksDone =0;
    int pidI = 0;
    private int count;
    boolean gettingPID =false;
    @Override
    public IBinder onBind(Intent intent)
    {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        count=0;
        BluetoothManage.getInstance(this).setBluetoothDataListener(this);
    }

    /**
     * Gets the Car's VIN
     */
    public void getCarVIN(){
        BluetoothManage.getInstance(this).obdGetParameter("2201");
    }

    public void startBluetoothSearch(){
        BluetoothManage.getInstance(this).connectBluetooth();
    }

    public int getState(){
        return BluetoothManage.getInstance(this).getState();
    }

    public void getPIDs(){
        BluetoothManage.getInstance(this).obdGetParameter("2401");
        gettingPID=true;
    }

    public void getDTCs(){
        BluetoothManage.getInstance(this).obdSetMonitor(1, "");
    }

    public void getFreeze(){
        BluetoothManage.getInstance(this).obdSetMonitor(3,"");
    }

    public String parseDTCs(String hex){
        int start = 1;
        char head = hex.charAt(0);
        HashMap<Character, String> map = new HashMap<Character, String>();map.put('0',"P0");
        map.put('1',"P1");
        map.put('2',"P2");
        map.put('3',"P3");

        map.put('4',"C0");
        map.put('5',"C1");
        map.put('6',"C2");
        map.put('7',"C3");

        map.put('8',"B0");
        map.put('9',"B1");
        map.put('A',"B2");
        map.put('B',"B3");

        map.put('C',"U0");
        map.put('D',"U1");
        map.put('E',"U2");
        map.put('F',"U3");
        return map.get(head)+hex.substring(start);
    }

    private void sendForPIDS(){
        String pid="";
        while(pidI!=pids.length){
            pid+=pids[pidI]+",";
            if  ((pidI+1)%9 ==0){
                BluetoothManage.getInstance(this)
                        .obdSetMonitor(4, pid.substring(0,pid.length()-1));
                pidI++;
                return;
            }else if ((pidI+1)==pids.length){
                BluetoothManage.getInstance(this)
                        .obdSetMonitor(4, pid.substring(0,pid.length()-1));
            }
            pidI++;
        }
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //BluetoothManage.getInstance(this).connectBluetooth();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "Destroyed",Toast.LENGTH_SHORT).show();
        super.onDestroy();
        BluetoothManage.getInstance(this).close();
    }

    @Override
    public void getBluetoothState(int state) {
//        if(state!=BluetoothManage.CONNECTED&&state!=BluetoothManage.BLUETOOTH_READ_DATA&&count==0)
//            Log.d("asdfasdfasd",""+state);
//            startBluetoothSearch();
        if(serviceCallbacks!=null)
            serviceCallbacks.getBluetoothState(state);
    }

    @Override
    public void setCtrlResponse(ResponsePackageInfo responsePackageInfo) {
        if(serviceCallbacks!=null)
            serviceCallbacks.setCtrlResponse(responsePackageInfo);
    }

    @Override
    public void setParamaterResponse(ResponsePackageInfo responsePackageInfo) {
        if(serviceCallbacks!=null)
            serviceCallbacks.setParamaterResponse(responsePackageInfo);

    }

    @Override
    public void getParamaterData(ParameterPackageInfo parameterPackageInfo) {
        if(gettingPID){
            pids  =parameterPackageInfo.value.get(0).value.split(",");
            pidI = 0;
            sendForPIDS();
            gettingPID=false;
        }else if(serviceCallbacks!=null)
            serviceCallbacks.getParamaterData(parameterPackageInfo);

    }

    @Override
    public void getIOData(DataPackageInfo dataPackageInfo) {

        if(pidI!=pids.length&&dataPackageInfo.result!=5){
            sendForPIDS();
        }
        if(dataPackageInfo.result==5){
            count++;
        }
        LocalDataRetriever ldr = new LocalDataRetriever(this);
        Responses response = new Responses();if(dataPackageInfo.result==1||dataPackageInfo.result==3||dataPackageInfo.result==4||dataPackageInfo.result==6||count%20==1) {
            if (count % 20 == 1)
                count = 1;
            response.setValue("result", "" + dataPackageInfo.result);
            response.setValue("deviceId", dataPackageInfo.deviceId);
            response.setValue("tripId", dataPackageInfo.tripId);
            response.setValue("dataNumber", dataPackageInfo.dataNumber);
            response.setValue("tripFlag", dataPackageInfo.tripFlag);
            response.setValue("rtcTime", dataPackageInfo.rtcTime);
            response.setValue("protocolType", dataPackageInfo.protocolType);
            response.setValue("tripMileage", dataPackageInfo.tripMileage);
            response.setValue("tripfuel", dataPackageInfo.tripfuel);
            response.setValue("vState", dataPackageInfo.vState);
            String OBD = "{";
            boolean recordedOnce = false;
            for (PIDInfo i : dataPackageInfo.obdData) {
                OBD += (recordedOnce ? ";'" : "'") + i.pidType + "':" + i.value;
                recordedOnce = true;
            }
            OBD += "}";
            response.setValue("OBD", OBD);
            String Freeze = "{";
            recordedOnce = false;
            for (PIDInfo i : dataPackageInfo.freezeData) {
                Freeze += (recordedOnce ? ";'" : "'") + i.pidType + "':" + i.value;
                recordedOnce = true;
            }
            Freeze += "}";
            response.setValue("Freeze", Freeze);
            response.setValue("supportPid", dataPackageInfo.surportPid);
            response.setValue("dtcData", dataPackageInfo.dtcData);
            ldr.saveData("Responses", response.getValues());
            if (serviceCallbacks != null)
                serviceCallbacks.getIOData(dataPackageInfo);
        }
    }

    public class BluetoothBinder extends Binder {
        public BluetoothAutoConnectService getService() {
            return BluetoothAutoConnectService.this;
        }
    }

    public void setCallbacks(BluetoothManage.BluetoothDataListener callbacks) {
        serviceCallbacks = callbacks;
    }

}
