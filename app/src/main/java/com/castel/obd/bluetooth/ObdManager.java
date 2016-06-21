package com.castel.obd.bluetooth;

import android.content.Context;
import android.util.Log;

import com.castel.obd.OBD;
import com.castel.obd.data.OBDInfoSP;
import com.castel.obd.info.BasePackageInfo;
import com.castel.obd.info.DataPackageInfo;
import com.castel.obd.info.LoginPackageInfo;
import com.castel.obd.info.ParameterPackageInfo;
import com.castel.obd.info.ResponsePackageInfo;
import com.castel.obd.info.SendPackageInfo;
import com.castel.obd.util.JsonUtil;
import com.castel.obd.util.LogUtil;
import com.castel.obd.util.Utils;
import com.pitstop.MainActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Paul Soladoye on 12/04/2016.
 */
public class ObdManager {
    public final static String BT_DEVICE_NAME = "IDD-212";
    public final static String RTC_TAG = "1A01";
    public final static String VIN_TAG = "2201";
    public final static String PID_TAG = "2401";

    public final static String TRIP_START_FLAG = "0";
    public final static String TRIP_END_FLAG = "9";

    public final static int DEVICE_LOGIN_FLAG = 1;
    public final static int DEVICE_LOGOUT_FLAG = 0;
    public final static int TYPE_DTC = 1;
    public final static int TYPE_PENDING_DTC = 2;
    public final static int TYPE_FREEZE_DATA = 3;

    private Context mContext;
    private IBluetoothDataListener dataListener;
    private IPassiveCommandListener passiveCommandListener;
    private boolean isParse = false;
    public List<DataPackageInfo> dataPackages;

    public ObdManager(Context context) {
        mContext = context;
        dataPackages = new ArrayList<>();
    }

    public void setDataListener(IBluetoothDataListener dataListener) {
        this.dataListener = dataListener;
    }

    public void setPassiveCommandListener(IPassiveCommandListener passiveCommandListener) {
        this.passiveCommandListener = passiveCommandListener;
    }

    public boolean isParse() {
        return isParse;
    }

    /**
     *
     */
    public int initializeObd() {
        Log.i(MainActivity.TAG, "Initializing obd");

        String deviceId = OBDInfoSP.getDeviceId(mContext);
        String dataNum = OBDInfoSP.getDataNum(mContext);

        if (!Utils.isEmpty(deviceId) && !Utils.isEmpty(dataNum)) {
            Log.i(MainActivity.TAG,"deviceId:" + deviceId + "dataNum"
                    + OBDInfoSP.getDataNum(mContext));
            Log.i(MainActivity.TAG,"Initializing obd module");
            return OBD.init(deviceId, dataNum);
        }
        return -1;
    }


    /**
     * @param type
     */
    public String obdSetCtrl(int type) {
        return OBD.setCtrl(type);
    }


    /**
     * @param type
     * @param valueList
     */
    public String obdSetMonitor(int type, String valueList) {
        return OBD.setMonitor(type, valueList);
    }


    /**
     * @param tlvTagList
     * @param valueList
     */
    public String obdSetParameter(String tlvTagList, String valueList) {
        return OBD.setParameter(tlvTagList, valueList);
    }


    /**
     * @param tlvTag
     */
    public String obdGetParameter(String tlvTag) {
        return OBD.getParameter(tlvTag);
    }


    /**
     * @param payload
     */
    public byte[] getBytesToSend(String payload) {
        if (Utils.isEmpty(payload)) {
            return null;
        }

        SendPackageInfo sendPackageInfo = JsonUtil.json2object(payload, SendPackageInfo.class);
        if (null == sendPackageInfo) {
            return null;
        }

        if (sendPackageInfo.result != 0
                || Utils.isEmpty(sendPackageInfo.instruction)) {
            return null;
        }

        return Utils.hexStringToBytes(sendPackageInfo.instruction);
    }

    /**
     * @param payload
     */
    public byte[] getBytesToSendPassive(String payload) {
        if (Utils.isEmpty(payload)) {
            return null;
        }

        return Utils.hexStringToBytes(payload);
    }

    /**
     * @param receivedPayload
     */
    public void receiveDataAndParse(String receivedPayload) {

        isParse = true;
        String info = OBD.getIOData(receivedPayload);
        isParse = false;

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
            Log.i(MainActivity.TAG,"Receiving result 0 - ObdManager");
            obdLoginPackageParse(info);
        } else if (2 == result) {
            Log.i(MainActivity.TAG,"Receiving result 2 - ObdManager");
            obdResponsePackageParse(info);
        } else if (3 == result) {
            Log.i(MainActivity.TAG,"Receiving result 3 - ObdManager");
            obdParameterPackageParse(info);
        } else if (4 == result || 5 == result || 6 == result) {
            Log.i(MainActivity.TAG,"Receiving result 4 or 5 or 6 - ObdManager");
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
        Log.i(MainActivity.TAG,"result: "+ parameterPackageInfo.result);
        Log.i(MainActivity.TAG, "Data: " + parameterPackageInfo.value.get(0).tlvTag);
        Log.i(MainActivity.TAG, "Data: " + parameterPackageInfo.value.get(0).value);
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
                Log.i(MainActivity.TAG, "Saving OBDInfo (DeviceId and DataNumber) - ObdManager");
                OBDInfoSP.saveInfo(mContext, dataPackageInfo.deviceId,
                        dataPackageInfo.dataNumber);
                Log.i(MainActivity.TAG,"dataNumber:" + dataPackageInfo.dataNumber);
            }

        }

        dataListener.getIOData(dataPackageInfo);
    }


    /**
     *
     */
    public interface IBluetoothDataListener {
        void getBluetoothState(int state);

        void setCtrlResponse(ResponsePackageInfo responsePackageInfo);

        void setParameterResponse(ResponsePackageInfo responsePackageInfo);

        void getParameterData(ParameterPackageInfo parameterPackageInfo);

        void getIOData(DataPackageInfo dataPackageInfo);

        void deviceLogin(LoginPackageInfo loginPackageInfo);
    }


    public interface IPassiveCommandListener {
        void sendCommandPassive(String payload);
    }
}
