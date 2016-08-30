package com.castel.obd.bleDevice;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.castel.obd.OBD;
import com.castel.obd.bluetooth.ObdManager;
import com.castel.obd.data.OBDInfoSP;
import com.castel.obd.info.BasePackageInfo;
import com.castel.obd.info.DataPackageInfo;
import com.castel.obd.info.LoginPackageInfo;
import com.castel.obd.info.ParameterPackageInfo;
import com.castel.obd.info.ResponsePackageInfo;
import com.castel.obd.util.JsonUtil;
import com.castel.obd.util.Utils;

import java.util.UUID;

/**
 * Created by Ben Wu on 2016-08-29.
 */
public class Device212B implements AbstractDevice {

    private static final String TAG = Device212B.class.getSimpleName();

    public final static String FIXED_UPLOAD_TAG = "1202,1201,1203,1204,1205,1206";
    public final static String RTC_TAG = "1A01";
    public final static String VIN_TAG = "2201";
    public final static String PID_TAG = "2401";

    public final static int TYPE_DTC = 1;
    public final static int TYPE_PENDING_DTC = 2;

    private ObdManager.IBluetoothDataListener dataListener;
    private ObdManager.IPassiveCommandListener passiveCommandListener;
    private Context context;

    public Device212B(Context context, ObdManager.IBluetoothDataListener dataListener,
                      ObdManager.IPassiveCommandListener passiveCommandListener) {
        this.dataListener = dataListener;
        this.context = context;
        this.passiveCommandListener = passiveCommandListener;
    }

    // functions

    @Override
    public UUID getServiceUuid() {
        return UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb"); // 212B
    }

    @Override
    public UUID getReadChar() {
        return UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb"); // 212B
    }

    @Override
    public UUID getWriteChar() {
        return UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb"); // 212B
    }

    @Override
    public byte[] getBytes(String payload) {
        if(Utils.isEmpty(payload)) {
            return null;
        }
        return Utils.hexStringToBytes(payload);
    }

    @Override
    public String getVin() {
        return OBD.getParameter(VIN_TAG);
    }

    @Override
    public String getRtc() {
        return OBD.getParameter(RTC_TAG);
    }

    @Override
    public String setRtc(long rtcTime) {
        return OBD.setParameter(ObdManager.RTC_TAG, String.valueOf(rtcTime / 1000));
    }

    @Override
    public String getSupportedPids() {
        return OBD.getParameter(PID_TAG);
    }

    @Override
    public String setPidsToSend(String pids) {
        return OBD.setParameter(FIXED_UPLOAD_TAG, "01;01;01;10;2;" + pids);
    }

    @Override
    public String getDtcs() {
        return OBD.setMonitor(TYPE_DTC, "");// + OBD.setMonitor(TYPE_PENDING_DTC, "");
    }

    // read data handler

    @Override
    public void onCharacteristicChanged(byte[] data) {
        final String readData = Utils.bytesToHexString(data);

        Log.d(TAG, "Data Read: " + readData);

        if(readData == null || readData.isEmpty()) {
            return;
        }
        receiveDataAndParse(readData);
    }

    @Override
    public void onCharacteristicRead(byte[] data, int status) {
        if(status != BluetoothGatt.GATT_SUCCESS) {
            return;
        }

        onCharacteristicChanged(data);
    }

    // data parsers

    /**
     * @param receivedPayload
     */
    public void receiveDataAndParse(String receivedPayload) {

        String info = OBD.getIOData(receivedPayload);

        info = info.replace("obdData\":]","obdData\":[]");
        String[] infos = info.split("&");

        for (int i = 0; i < infos.length; i++) {
            BasePackageInfo baseInfo = JsonUtil.json2object(infos[i],
                    BasePackageInfo.class);
            if (null != baseInfo) {
                determinePackageType(infos[i], baseInfo.result);
            }
        }
    }

    /**
     * @param info
     * @param result
     */
    private void determinePackageType(String info, int result) {
        if (0 == result) {
            Log.d(TAG,"Receiving result 0 - ObdManager");
            obdLoginPackageParse(info);
        } else if (2 == result) {
            Log.d(TAG,"Receiving result 2 - ObdManager");
            obdResponsePackageParse(info);
        } else if (3 == result) {
            Log.d(TAG,"Receiving result 3 - ObdManager");
            obdParameterPackageParse(info);
        } else if (4 == result || 5 == result || 6 == result) {
            Log.d(TAG,"Receiving result 4 or 5 or 6 - ObdManager");
            obdIODataPackageParse(info);
        }
    }


    /**
     * @param info
     */
    private void obdLoginPackageParse(String info) {
        LoginPackageInfo loginPackageInfo = JsonUtil.json2object(info,
                LoginPackageInfo.class);

        if (null == loginPackageInfo) {
            return;
        }

        if ("0".equals(loginPackageInfo.flag)) {
            dataListener.deviceLogin(loginPackageInfo);
        } else if ("1".equals(loginPackageInfo.flag)) {
            passiveCommandListener.sendCommandPassive(loginPackageInfo.instruction);
            dataListener.deviceLogin(loginPackageInfo);
        }
    }


    /**
     * @param info
     */
    private void obdResponsePackageParse(String info) {
        ResponsePackageInfo responsePackageInfo = JsonUtil.json2object(info,
                ResponsePackageInfo.class);

        if (null == responsePackageInfo) {
            dataListener.setCtrlResponse(null);
            dataListener.setParameterResponse(null);
        } else {
            if ("0".equals(responsePackageInfo.flag)) {
                dataListener.setCtrlResponse(responsePackageInfo);
            } else if ("1".equals(responsePackageInfo.flag)) {
                dataListener.setParameterResponse(responsePackageInfo);
            }
        }
    }


    /**
     * @param info
     */

    private void obdParameterPackageParse(String info) {
        ParameterPackageInfo parameterPackageInfo = JsonUtil.json2object(info,
                ParameterPackageInfo.class);
        dataListener.getParameterData(parameterPackageInfo);
        Log.i(TAG,"result: "+ parameterPackageInfo.result);
        Log.i(TAG, "Data: " + parameterPackageInfo.value.get(0).tlvTag);
        Log.i(TAG, "Data: " + parameterPackageInfo.value.get(0).value);
    }


    /**
     * @param info
     */
    public void obdIODataPackageParse(String info) {
        DataPackageInfo dataPackageInfo = JsonUtil.json2object(info,
                DataPackageInfo.class);

        if (null != dataPackageInfo) {
            //dataPackages.add(dataPackageInfo);

            if (!Utils.isEmpty(dataPackageInfo.dataNumber)) {
                Log.i(TAG, "Saving OBDInfo (DeviceId and DataNumber) - ObdManager");
                OBDInfoSP.saveInfo(context, dataPackageInfo.deviceId,
                        dataPackageInfo.dataNumber);
                Log.i(TAG,"dataNumber:" + dataPackageInfo.dataNumber);
            }

        }

        dataListener.getIOData(dataPackageInfo);
    }
}
