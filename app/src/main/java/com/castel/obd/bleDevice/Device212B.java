package com.castel.obd.bleDevice;

import android.content.Context;
import android.util.Log;

import com.castel.obd.OBD;
import com.castel.obd.bluetooth.ObdManager;
import com.castel.obd.data.OBDInfoSP;
import com.castel.obd.info.BasePackageInfo;
import com.castel.obd.info.DataPackageInfo;
import com.castel.obd.info.LoginPackageInfo;
import com.castel.obd.info.PIDInfo;
import com.castel.obd.info.ParameterPackageInfo;
import com.castel.obd.info.ResponsePackageInfo;
import com.castel.obd.util.JsonUtil;
import com.castel.obd.util.ObdDataUtil;
import com.castel.obd.util.Utils;
import com.pitstop.bluetooth.BluetoothDeviceManager;
import com.pitstop.bluetooth.dataPackages.DtcPackage;
import com.pitstop.bluetooth.dataPackages.FreezeFramePackage;
import com.pitstop.bluetooth.dataPackages.ParameterPackage;
import com.pitstop.bluetooth.dataPackages.PidPackage;
import com.pitstop.bluetooth.dataPackages.TripInfoPackage;

import java.util.ArrayList;
import java.util.HashMap;
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
    public final static int TYPE_FREEZE_DATA = 3;

    private ObdManager.IBluetoothDataListener dataListener;
    private ObdManager.IPassiveCommandListener passiveCommandListener;
    private Context context;
    private final String deviceName;

    public Device212B(Context context, ObdManager.IBluetoothDataListener dataListener,
                      ObdManager.IPassiveCommandListener passiveCommandListener, String deviceName) {
        this.dataListener = dataListener;
        this.context = context;
        this.passiveCommandListener = passiveCommandListener;
        this.deviceName = deviceName;
    }

    // functions

    @Override
    public BluetoothDeviceManager.CommType commType() {
        return BluetoothDeviceManager.CommType.CLASSIC;
    }

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
    public String requestData() {
        return null;
    }

    @Override
    public String getDeviceName() {
        return deviceName;
    }

    @Override
    public String getVin() {
        return OBD.getParameter(VIN_TAG);
    }
//TODO: change this too
    @Override
    public String getRtc() {
        return OBD.getParameter(RTC_TAG);
    }

    @Override
    public String setRtc(long rtcTime) {
        return OBD.setParameter(ObdManager.RTC_TAG, String.valueOf(rtcTime / 1000));
    }

    @Override
    public String getPids(String pids) {
        return ""; // 212 does not need to explicitly get pids
    }

    @Override
    public String getSupportedPids() {
        return OBD.getParameter(PID_TAG);
    }

    @Override
    public String setPidsToSend(String pids, int timeInterval) {
        return OBD.setParameter(FIXED_UPLOAD_TAG, "01;01;01;10;2;" + pids);
    }

    @Override
    public String requestSnapshot() {
        return null; //Not supported
    }

    @Override
    public String getDtcs() {
        return OBD.setMonitor(TYPE_DTC, "");
    }

    @Override
    public String getPendingDtcs() {
        return OBD.setMonitor(TYPE_PENDING_DTC, "");
    }

    @Override
    public String getFreezeFrame() {
        return OBD.setMonitor(TYPE_FREEZE_DATA, "");
    }

    // read data handler

    @Override
    public void parseData(byte[] data) {
        final String readData = Utils.bytesToHexString(data);

        Log.d(TAG, "Data Read: " + readData);

        if(readData == null || readData.isEmpty()) {
            return;
        }
        receiveDataAndParse(readData);
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
        // 212 specific package
        ParameterPackageInfo parameterPackageInfo = JsonUtil.json2object(info,
                ParameterPackageInfo.class);
        //dataListener.getParameterData(parameterPackageInfo);

        //generic parameter package
        ParameterPackage parameterPackage = new ParameterPackage();
        parameterPackage.deviceId = parameterPackageInfo.deviceId;
        parameterPackage.success = parameterPackageInfo.result == 0;

        if(parameterPackageInfo.value != null && parameterPackageInfo.value.size() > 0) {
            parameterPackage.paramType = getParamType(parameterPackageInfo.value.get(0).tlvTag);
            parameterPackage.value = parameterPackageInfo.value.get(0).value;

            if(parameterPackage.paramType == null) {
                Log.w(TAG, "Unrecognized tlv tag: " + parameterPackageInfo.value.get(0).tlvTag);
            }
        } else {
            parameterPackage.success = false;
        }

        dataListener.parameterData(parameterPackage);

        Log.i(TAG,"result: "+ parameterPackageInfo.result);
        Log.i(TAG, "Data: " + parameterPackageInfo.value.get(0).tlvTag);
        Log.i(TAG, "Data: " + parameterPackageInfo.value.get(0).value);
    }

    private ParameterPackage.ParamType getParamType(String tlvTag) {
        switch(tlvTag) {
            case PID_TAG:
                return ParameterPackage.ParamType.SUPPORTED_PIDS;
            case RTC_TAG:
                return ParameterPackage.ParamType.RTC_TIME;
            case VIN_TAG:
                return ParameterPackage.ParamType.VIN;
            default:
                return null;
        }
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
                Log.i(TAG, "dataNumber:" + dataPackageInfo.dataNumber);
            }

            final String tripFlag = dataPackageInfo.tripFlag != null ? dataPackageInfo.tripFlag : "-1";

            // process dtc data
            if ((dataPackageInfo.dtcData != null && dataPackageInfo.result == 6) ||
                    (tripFlag.equals("6") || tripFlag.equals("5")) && dataPackageInfo.dtcData != null) {

                Log.i(TAG, "Parsing DTC data");

                DtcPackage dtcPackage = new DtcPackage();

                boolean isPending = tripFlag.equals("5");

                dtcPackage.rtcTime = dataPackageInfo.rtcTime;

                dtcPackage.deviceId = dataPackageInfo.deviceId;

                if(dataPackageInfo.dtcData.isEmpty()) {
                    dtcPackage.dtcs = new HashMap<>();
                } else {

                    String[] unparsedDtcs = dataPackageInfo.dtcData.split(",");

                    dtcPackage.dtcs = new HashMap<>();

                    for (int i = 0; i < unparsedDtcs.length; i++) {
                        if (unparsedDtcs[i].length() > 0) {
                            dtcPackage.dtcs.put(ObdDataUtil.parseDTCs(unparsedDtcs[i]),isPending);
                        }
                    }
                }

                dataListener.dtcData(dtcPackage);
            }

            if(dataPackageInfo.result == 5) {
                Log.d(TAG, "Result 5 PIDs");
                dataListener.pidData(null);
            }

            // fixed upload pids
            if(dataPackageInfo.result == 4 && tripFlag.equals("1")
                    && dataPackageInfo.obdData != null && dataPackageInfo.obdData.size() > 0) {
                Log.d(TAG, "Result 4 PIDs");
                PidPackage templatePidPackage = new PidPackage();

                templatePidPackage.deviceId = dataPackageInfo.deviceId;
                templatePidPackage.tripMileage = dataPackageInfo.tripMileage;
                templatePidPackage.tripId = dataPackageInfo.tripId;
                templatePidPackage.rtcTime = dataPackageInfo.rtcTime;
                templatePidPackage.timestamp = String.valueOf(System.currentTimeMillis() / 1000);
                templatePidPackage.realTime = false;

                // pid map for aggregated pid
                HashMap<String, String[]> aggregatePidMap = new HashMap<>();

                int numberOfDataPoints = dataPackageInfo.obdData.get(0).value.split(",").length;

                // parse aggregate data to individual pid snapshots
                // e.g. {"id":"210D","data":"87,87,87,87,87"} to 5 {"id":"210D","data":"87"}
                for (PIDInfo pidInfo : dataPackageInfo.obdData) {
                    aggregatePidMap.put(pidInfo.pidType, pidInfo.value.split(","));
                }

                // maps for individual pid data points
                ArrayList<HashMap<String, String>> pidMapList = new ArrayList<>();

                for (int i = 0; i < numberOfDataPoints; i++) {
                    pidMapList.add(new HashMap<String, String>());
                }

                for (PIDInfo pidInfo : dataPackageInfo.obdData) {
                    String[] aggregateValues = aggregatePidMap.get(pidInfo.pidType);
                    for (int i = 0; i < numberOfDataPoints; i++) {
                        HashMap<String, String> mapToUpdate = pidMapList.get(i);
                        mapToUpdate.put(pidInfo.pidType, aggregateValues[i]);
                        pidMapList.set(i, mapToUpdate);
                    }
                }

                for (int i = 0; i < pidMapList.size(); i++) {
                    PidPackage pidPackage = new PidPackage(templatePidPackage);
                    pidPackage.rtcTime = String.valueOf(Long.parseLong(pidPackage.rtcTime) - 2 * (numberOfDataPoints - i - 1));
                    pidPackage.pids = pidMapList.get(i);
                    dataListener.idrPidData(pidPackage);
                }
            }

            // trip start or trip end flag or real-time data
            if(((dataPackageInfo.result == 4 && (tripFlag.equals("0") || tripFlag.equals("9"))) ||
                    dataPackageInfo.result == 5) && !dataPackageInfo.tripMileage.isEmpty()) {
                TripInfoPackage tripInfoPackage = new TripInfoPackage();
                tripInfoPackage.deviceId = dataPackageInfo.deviceId;
                try {
                    tripInfoPackage.tripId = Integer.parseInt(dataPackageInfo.tripId);
                } catch(NumberFormatException e) {
                    tripInfoPackage.tripId = 0;
                }
                tripInfoPackage.mileage = Double.parseDouble(dataPackageInfo.tripMileage) / 1000;
                tripInfoPackage.rtcTime = Long.parseLong(dataPackageInfo.rtcTime);

                if(dataPackageInfo.result == 5) {
                    tripInfoPackage.flag = TripInfoPackage.TripFlag.UPDATE;
                } else if(tripFlag.equals("0")) {
                    tripInfoPackage.flag = TripInfoPackage.TripFlag.START;
                } else if(tripFlag.equals("9")) {
                    tripInfoPackage.flag = TripInfoPackage.TripFlag.END;
                }
                dataListener.tripData(tripInfoPackage);
            }

            // handle freeze frame data
            if (dataPackageInfo.freezeData != null && !dataPackageInfo.freezeData.isEmpty()){ // TODO: 16/12/13 Check what happen if result 6 is removed
                if (/*dataPackageInfo.result == 6 || */(dataPackageInfo.result == 4 && ObdManager.FREEZE_FRAME_FLAG.equals(dataPackageInfo.tripFlag))){
                    FreezeFramePackage ffPackage = new FreezeFramePackage();
                    ffPackage.deviceId = dataPackageInfo.deviceId;
                    ffPackage.rtcTime = Long.parseLong(dataPackageInfo.rtcTime);
                    ffPackage.freezeData = new HashMap<>();
                    for (PIDInfo pidInfo: dataPackageInfo.freezeData){
                        ffPackage.freezeData.put(pidInfo.pidType, pidInfo.value);
                    }
                    dataListener.ffData(ffPackage);
                }
            }

        }


//        dataListener.getIOData(dataPackageInfo);
    }

    @Override
    public String resetDeviceToDefaults() {
        //TODO
        return null;
    }

    @Override
    public String resetDevice() {
        //TODO
        return null;
    }

    @Override
    public String clearDeviceMemory() {
        //TODO: add this
        return null;
    }

    @Override
    public String clearDtcs() {
        //TODO
        return null;
    }
}
