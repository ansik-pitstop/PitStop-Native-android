package com.castel.obd.bleDevice;

import android.content.Context;
import android.util.Log;

import com.castel.obd.OBD;
import com.castel.obd215b.info.FaultInfo;
import com.castel.obd215b.info.PIDInfo;
import com.castel.obd215b.util.FaultParse;
import com.castel.obd215b.util.Utils;
import com.pitstop.bluetooth.BluetoothDeviceManager;
import com.castel.obd.bluetooth.ObdManager;
import com.castel.obd.info.DataPackageInfo;
import com.castel.obd215b.info.DTCInfo;
import com.castel.obd215b.info.IDRInfo;
import com.castel.obd215b.info.SettingInfo;
import com.castel.obd215b.util.Constants;
import com.castel.obd215b.util.DataParseUtil;
import com.castel.obd215b.util.DateUtil;
import com.pitstop.bluetooth.dataPackages.DtcPackage;
import com.pitstop.bluetooth.dataPackages.FreezeFramePackage;
import com.pitstop.bluetooth.dataPackages.ParameterPackage;
import com.pitstop.bluetooth.dataPackages.PidPackage;
import com.pitstop.bluetooth.dataPackages.TripInfoPackage;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Created by Ben Wu on 2016-08-29.
 */
public class Device215B implements AbstractDevice {

    private static final String TAG = Device215B.class.getSimpleName();

    // parameter IDs
    public static final String TERMINAL_ID_PARAM = "A01";
    public static final String BT_NAME_PARAM = "A02";
    public static final String RTC_TIME_PARAM = "A03";
    public static final String VIN_PARAM = "A07";
    public static final String SAMPLED_PID_PARAM = "A14";
    public static final String IDR_INTERVAL_PARAM = "A15";
    public static final String HISTORICAL_DATA_PARAM = "A18";

    ObdManager.IBluetoothDataListener dataListener;
    private Context context;
    private final String deviceName;

    public Device215B(Context context, ObdManager.IBluetoothDataListener dataListener, String deviceName) {
        this.dataListener = dataListener;
        this.context = context;
        this.deviceName = deviceName;
    }

    // functions

    @Override
    public BluetoothDeviceManager.CommType commType() {
        return BluetoothDeviceManager.CommType.LE;
    }

    @Override
    public UUID getServiceUuid() {
        return UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e"); // 215B
    }

    @Override
    public UUID getReadChar() {
        return UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e"); // 215B
    }

    @Override
    public UUID getWriteChar() {
        return UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e"); // 215B
    }

    @Override
    public byte[] getBytes(String payload) {
        return payload.getBytes();
    }

    @Override
    public String requestData() {
        return replyIDRPackage();
    }

    @Override
    public String getDeviceName() {
        return deviceName;
    }

    @Override
    public String getVin() {
        return qiSingle(VIN_PARAM);
    }

    @Override
    public String getRtc() {
        return qiSingle(RTC_TIME_PARAM);
    }

    @Override
    public String setRtc(long rtcTime) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(rtcTime);
        String dateString = new SimpleDateFormat("yyMMddHHmmss").format(calendar.getTime());

        return siSingle(RTC_TIME_PARAM, dateString);
    }

    @Override
    public String getPids(String pids) {
        int count = pids.split(",").length;
        return pidPackage("0", count, pids, "0");
    }

    @Override
    public String getSupportedPids() {
        return pidtPackage("0");
    }

    @Override
    public String setPidsToSend(String pids) {
        return siMulti(SAMPLED_PID_PARAM + "," + IDR_INTERVAL_PARAM,  pids.replace(",", "/") + ",2");
    }

    @Override
    public String getDtcs() {
        return dtcPackage("0", "0");
    }

    @Override
    public String getPendingDtcs() {
        return "";
    }

    @Override
    public String getFreezeFrame() {
        return null; // 215B does not require explicit command to read FF
    }

    // read data handler

    @Override
    public void parseData(byte[] data) {
        String readData = "";

        try {
            readData = new String(data, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        //Log.v(TAG, "Data Read: " + readData.replace("\r", "\\r").replace("\n", "\\n"));

        if(readData.isEmpty()) {
            return;
        }

        parseReadData(readData);
    }

    public String replyIDRPackage() {
        StringBuilder sb = new StringBuilder();
        sb.append(Constants.INSTRUCTION_HEAD);
        sb.append("0,");
        sb.append(Constants.INSTRUCTION_IDR);
        sb.append(",0,");
        sb.append(Constants.INSTRUCTION_STAR);

        String crcData = sb.toString();
        String crc = Utils.toHexString(OBD.CRC(crcData));

        String msg = crcData + crc + Constants.INSTRUCTION_FOOD;

        return msg;
    }

    private String qiSingle(String param) {
        String crcData = Constants.INSTRUCTION_HEAD
                + "0"
                + ","
                + Constants.INSTRUCTION_QI
                + ",2,A01,"
                + param
                + ","
                + Constants.INSTRUCTION_STAR;

        String crc = Utils.toHexString(OBD.CRC(crcData));

        String msg = crcData + crc + Constants.INSTRUCTION_FOOD;

        return msg;
    }

    private String siSingle(String param, String value) {
        String crcData = Constants.INSTRUCTION_HEAD
                + "0"
                + ","
                + Constants.INSTRUCTION_SI
                + ",1,"
                + param
                + ","
                + value
                + ","
                + Constants.INSTRUCTION_STAR;

        String crc = Utils.toHexString(OBD.CRC(crcData));

        String msg = crcData + crc + Constants.INSTRUCTION_FOOD;

        return msg;
    }

    private String siMulti(String params, String values) {
        String[] splitParams = params.split(",");
        String[] splitValues = values.split(",");

        int numberOfParams = splitParams.length;

        if (splitValues.length != numberOfParams) {
            Log.w(TAG, "siMulti: Number of params and values must match");
            return "";
        }

        StringBuilder paramAndValues = new StringBuilder();

        for(int i = 0 ; i < numberOfParams ; i++) {
            paramAndValues.append(splitParams[i]);
            paramAndValues.append(",");
            paramAndValues.append(splitValues[i]);
            paramAndValues.append(",");

        }

        String crcData = Constants.INSTRUCTION_HEAD
                + "0"
                + ","
                + Constants.INSTRUCTION_SI
                + ","
                + numberOfParams
                + ","
                + paramAndValues.toString()
                + Constants.INSTRUCTION_STAR;

        String crc = com.castel.obd215b.util.Utils.toHexString(OBD.CRC(crcData));

        String msg = crcData + crc + Constants.INSTRUCTION_FOOD;

        return msg;
    }

    private String pidPackage(String controlEventID, int pidNum,
                                    String pids, String terminalSN) {
        String crcData = "";
        if (0 == pidNum) {
            crcData = Constants.INSTRUCTION_HEAD + terminalSN + ","
                    + Constants.INSTRUCTION_PID + "," + controlEventID + ","
                    + Constants.INSTRUCTION_STAR;
        } else {
            crcData = Constants.INSTRUCTION_HEAD + terminalSN + ","
                    + Constants.INSTRUCTION_PID + "," + controlEventID + ","
                    + pidNum + "," + pids + "," + Constants.INSTRUCTION_STAR;
        }

        // String crc = Integer.toHexString(OBD.CRC(crcData)).toUpperCase();
        String crc = com.castel.obd215b.util.Utils.toHexString(OBD.CRC(crcData));

        String msg = crcData + crc + Constants.INSTRUCTION_FOOD;

        return msg;
    }

    public static String dtcPackage(String controlEventID, String terminalSN) {
        String crcData = Constants.INSTRUCTION_HEAD + terminalSN + ","
                + Constants.INSTRUCTION_DTC + "," + controlEventID + ","
                + Constants.INSTRUCTION_STAR;

        // String crc = Integer.toHexString(OBD.CRC(crcData)).toUpperCase();
        String crc = com.castel.obd215b.util.Utils.toHexString(OBD.CRC(crcData));

        String msg = crcData + crc + Constants.INSTRUCTION_FOOD;

        return msg;
    }

    public static String pidtPackage(String terminalSN) {
        String crcData = Constants.INSTRUCTION_HEAD + terminalSN + ","
                + Constants.INSTRUCTION_PIDT + "," + Constants.INSTRUCTION_STAR;

        // String crc = Integer.toHexString(OBD.CRC(crcData)).toUpperCase();
        String crc = com.castel.obd215b.util.Utils.toHexString(OBD.CRC(crcData));

        String msg = crcData + crc + Constants.INSTRUCTION_FOOD;

        return msg;
    }

    private StringBuilder sbRead = new StringBuilder();

    // parser for 215B data
    private void parseReadData(String msg) {
        sbRead.append(msg);

        if (sbRead.toString().contains("\r\n")) {
            String msgInfo = sbRead.toString().replace("\r\n",
                    "\\r\\n");

            Log.v(TAG, "Data Read: " + msgInfo);

            sbRead = new StringBuilder();

            // determine response type
            if (Constants.INSTRUCTION_IDR.equals(DataParseUtil
                    .parseMsgType(msgInfo))) {

                String dateStr = DateUtil.getSystemTime("yyyy-MM-dd HH:mm:ss");

                IDRInfo idrInfo = DataParseUtil.parseIDR(msgInfo);
                idrInfo.time = dateStr;

                long ignitionTime; // ignition time parsed as unix time seconds
                try {
                    ignitionTime = parseRtcTime(idrInfo.ignitionTime);
                } catch (ParseException e) {
                    e.printStackTrace();
                    ignitionTime = 0;
                }

                // Trip end/start
                if(idrInfo.mileage != null && !idrInfo.mileage.isEmpty()) {
                    TripInfoPackage tripInfoPackage = new TripInfoPackage();
                    tripInfoPackage.deviceId = idrInfo.terminalSN;
                    tripInfoPackage.rtcTime = ignitionTime + Long.parseLong(idrInfo.runTime);
                    tripInfoPackage.tripId = (int) ignitionTime;
                    tripInfoPackage.flag = TripInfoPackage.TripFlag.UPDATE;
                    tripInfoPackage.mileage = Double.parseDouble(idrInfo.mileage) / 1000;

                    dataListener.tripData(tripInfoPackage);
                }

                if(idrInfo.pid != null && !idrInfo.pid.isEmpty()) {
                    PidPackage pidPackage = new PidPackage();
                    try {
                        pidPackage.rtcTime = String.valueOf(parseRtcTime(idrInfo.ignitionTime)
                                + Long.parseLong(idrInfo.runTime));
                    } catch (ParseException e) {
                        e.printStackTrace();
                        pidPackage.rtcTime = "0";
                    }
                    pidPackage.pids = parsePids(idrInfo.pid);
                    pidPackage.tripMileage = String.valueOf(Double.parseDouble(idrInfo.mileage) / 1000);
                    pidPackage.deviceId = idrInfo.terminalSN;
                    pidPackage.timestamp = String.valueOf(System.currentTimeMillis() / 1000);
                    pidPackage.realTime = true;
                    try {
                        pidPackage.tripId = String.valueOf(parseRtcTime(idrInfo.ignitionTime));
                    } catch (ParseException e) {
                        e.printStackTrace();
                        pidPackage.tripId = "0";
                    }

                    dataListener.pidData(pidPackage);
                } else { // to request a new IDR package
                    PidPackage pidPackage = new PidPackage();
                    pidPackage.realTime = false;
                    dataListener.pidData(pidPackage);
                }

                if(idrInfo.dtc != null && !idrInfo.dtc.isEmpty()) {
                    // dtc example: "0/07470107/07470207/07470307/07474307"

                    String[] unparsedDtcCodes = idrInfo.dtc.split("/");

                    if(unparsedDtcCodes.length > 1) {
                        Log.i(TAG, "Parsing DTC data: " + idrInfo.dtc);

                        DtcPackage dtcPackage = new DtcPackage();

                        dtcPackage.isPending = idrInfo.dtc.charAt(0) == '1';

                        // first element is dtc type
                        for(int i = 1 ; i < unparsedDtcCodes.length ; i++) {
                            unparsedDtcCodes[i] = unparsedDtcCodes[i].substring(4);
                        }

                        List<FaultInfo> faultInfo = FaultParse.parse(context, unparsedDtcCodes);

                        String[] dtcCodes = new String[faultInfo.size()];

                        for(int i = 1 ; i < faultInfo.size() ; i++) {
                            dtcCodes[i] = faultInfo.get(i).code;
                        }

                        dtcPackage.dtcs = dtcCodes;

                        dtcPackage.dtcNumber = dtcCodes.length;

                        try {
                            dtcPackage.rtcTime = String.valueOf(parseRtcTime(idrInfo.ignitionTime)
                                    + Long.parseLong(idrInfo.runTime));
                        } catch (ParseException e) {
                            e.printStackTrace();
                            dtcPackage.rtcTime = "0";
                        }

                        dtcPackage.deviceId = idrInfo.terminalSN;

                        dataListener.dtcData(dtcPackage);
                    }
                }

                if (idrInfo.freezeFrame != null && !idrInfo.freezeFrame.isEmpty()){
                    Log.i("FreezeFrame", idrInfo.freezeFrame);
                    // e.g. 25/2102/0001/2103/0002/2104/00/2105/D8/2106/64/2107/64/210B/00/210C/0003/
                    // 210D/00/210E/C0/210F/D8/2110/0056/2111/00/211F/0000/212E/00/212F/00/2133/00/
                    // 2142/0031/2143/02/2144/0080/2145/00/2147/00/2149/00/214A/00/214C/00
                    String[] unparsedFFCodes = idrInfo.freezeFrame.split("/");
                    if (unparsedFFCodes.length > 1 && unparsedFFCodes.length % 2 == 1) {
                        try {
                            Log.i(TAG, "Parsing FF data: " + idrInfo.freezeFrame);
                            FreezeFramePackage ffPackage = new FreezeFramePackage();
                            ffPackage.rtcTime = parseRtcTime(idrInfo.ignitionTime) + Long.parseLong(idrInfo.runTime);
                            ffPackage.deviceId = idrInfo.terminalSN;
                            ffPackage.freezeData = parseFreezeFrame(unparsedFFCodes);
                            dataListener.ffData(ffPackage);
                        } catch (Exception e){
                            e.printStackTrace();
                            Log.e(TAG, "Parsing freeze frame error! " + idrInfo.freezeFrame);
                        }
                    }
                }

                if (idrInfo.snapshot != null && !idrInfo.snapshot.isEmpty()){
                    Log.i("SnapShot", idrInfo.snapshot);
                }

                Log.d(TAG, idrInfo.toString());

            } else if (Constants.INSTRUCTION_SI
                    .equals(DataParseUtil.parseMsgType(msgInfo))) {
                boolean bResult = DataParseUtil
                        .parseSetting(msgInfo);

                Log.i(TAG, "SI result: " + bResult);

                //dataListener.setParameterResponse();
            } else if (Constants.INSTRUCTION_QI
                    .equals(DataParseUtil.parseMsgType(msgInfo))) {
                SettingInfo settingInfo = DataParseUtil
                        .parseQI(msgInfo);

                ParameterPackage parameterPackage = new ParameterPackage();
                parameterPackage.deviceId = settingInfo.terminalSN;

                // assumes only one parameter queried per command
                if(settingInfo.terminalRTCTime != null) {
                    try {
                        long rtcTime = parseRtcTime(settingInfo.terminalRTCTime);
                        parameterPackage.success = true;
                        parameterPackage.paramType = ParameterPackage.ParamType.RTC_TIME;
                        parameterPackage.value = String.valueOf(rtcTime);
                    } catch (ParseException e) {
                        e.printStackTrace();
                        parameterPackage.success = false;
                    }
                } else if(settingInfo.vehicleVINCode != null) {
                    parameterPackage.success = true;
                    parameterPackage.paramType = ParameterPackage.ParamType.VIN;
                    parameterPackage.value = settingInfo.vehicleVINCode;
                } else {
                    parameterPackage.success = false;
                }

                dataListener.parameterData(parameterPackage);
            } else if (Constants.INSTRUCTION_PIDT
                    .equals(DataParseUtil.parseMsgType(msgInfo))) {
                PIDInfo pidInfo = DataParseUtil.parsePIDT(msgInfo);

                ParameterPackage parameterPackage = new ParameterPackage();
                parameterPackage.deviceId = pidInfo.terminalId;

                StringBuilder sb = new StringBuilder();

                for(String pid : pidInfo.pids) { // rebuild pid string
                    sb.append(pid);
                    sb.append(",");
                }
                if(sb.length() > 0) {
                    sb.deleteCharAt(sb.length() - 1);
                    parameterPackage.value = sb.toString();
                    parameterPackage.paramType = ParameterPackage.ParamType.SUPPORTED_PIDS;
                } else {
                    parameterPackage.success = false;
                }

                dataListener.parameterData(parameterPackage);
            } else if (Constants.INSTRUCTION_PID
                    .equals(DataParseUtil.parseMsgType(msgInfo))) {
                // PIDs are never explicitly requested except in debug activity
                PIDInfo pidInfo = DataParseUtil.parsePID(msgInfo);
                PidPackage pidPackage = new PidPackage();
                HashMap<String, String> pidMap = new HashMap<>();

                for(int i = 0 ; i < pidInfo.pids.size() ; i++) {
                    pidMap.put(pidInfo.pids.get(i), pidInfo.pidValues.get(i));
                }

                pidPackage.pids = pidMap;
                pidPackage.realTime = false;
                pidPackage.tripId = "0";
                pidPackage.rtcTime = "0";
                pidPackage.deviceId = pidInfo.terminalId;
                pidPackage.tripMileage = "0";
                pidPackage.timestamp = String.valueOf(System.currentTimeMillis() / 1000);

                dataListener.pidData(pidPackage);
            } else if (Constants.INSTRUCTION_DTC
                    .equals(DataParseUtil.parseMsgType(msgInfo))) {
                Log.i(TAG, msgInfo);
                DTCInfo dtcInfo = DataParseUtil.parseDTC(msgInfo);

                Log.i(TAG, dtcInfo.toString());

                Log.i(TAG, "Parsing DTC data");

                DtcPackage dtcPackage = new DtcPackage();

                dtcPackage.isPending = dtcInfo.dtcType == 1;

                // dtc example:
                // $$HT-IDD215B-S00V002,DTC,1,04,4,07470107,07470207,07470307,07474307,*308F\r\n
                // DTCInfo [deviceId=HT-IDD215B-S00V002, dtcType=1, diagnosisProtocol=04, dtcNumber=4, dtcs=[07470107, 07470207, 07470307, 07474307]]

                String[] unparsedDtcCodes = new String[dtcInfo.dtcs != null ? dtcInfo.dtcs.length : 0];

                for(int i = 0 ; i < unparsedDtcCodes.length ; i++) {
                    unparsedDtcCodes[i] = dtcInfo.dtcs[i].substring(4);
                }

                List<FaultInfo> faultInfo = FaultParse.parse(context, unparsedDtcCodes);

                String[] dtcCodes = new String[faultInfo.size()];

                for(int i = 0 ; i < faultInfo.size() ; i++) {
                    dtcCodes[i] = faultInfo.get(i).code;
                }

                dtcPackage.dtcs = dtcCodes;

                dtcPackage.dtcNumber = dtcCodes.length;

                dtcPackage.rtcTime = String.valueOf(System.currentTimeMillis() / 1000);

                dtcPackage.deviceId = dtcInfo.terminalId;

                dataListener.dtcData(dtcPackage);
            }
        }
    }

    private long parseRtcTime(String rtcTime) throws ParseException {
        return new SimpleDateFormat("yyMMddHHmmss").parse(rtcTime).getTime() / 1000;
    }

    private HashMap<String, String> parsePids(String unparsedPidString) {
        HashMap<String, String> pidMap = new HashMap<>();

        // unparsedPidString should look like "2105/7C/210C/2ED3/210D/0A/210F/19/2110/BCAB/212F/32"

        String[] pidArray = unparsedPidString.split("/");

        for(int i = 0 ; i + 1 < pidArray.length ; i += 2) {
            pidMap.put(pidArray[i], pidArray[i + 1]);
        }

        return pidMap;
    }

    private HashMap<String, String> parseFreezeFrame(String[] pidArray) {
        HashMap<String, String> pidMap = new HashMap<>();
        for (int i = 1; i < pidArray.length - 1; i += 2) {
            pidMap.put(pidArray[i], pidArray[i + 1]);
        }
        return pidMap;
    }
}
