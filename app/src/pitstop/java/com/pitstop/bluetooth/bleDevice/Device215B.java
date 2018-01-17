package com.pitstop.bluetooth.bleDevice;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import com.castel.obd.OBD;
import com.pitstop.bluetooth.communicator.BluetoothCommunicator;
import com.pitstop.bluetooth.communicator.BluetoothLeComm;
import com.pitstop.bluetooth.communicator.IBluetoothCommunicator;
import com.pitstop.bluetooth.communicator.ObdManager;
import com.castel.obd215b.info.DTCInfo;
import com.castel.obd215b.info.FaultInfo;
import com.castel.obd215b.info.IDRInfo;
import com.castel.obd215b.info.PIDInfo;
import com.castel.obd215b.info.SettingInfo;
import com.castel.obd215b.util.Constants;
import com.castel.obd215b.util.DataParseUtil;
import com.castel.obd215b.util.DateUtil;
import com.castel.obd215b.util.FaultParse;
import com.castel.obd215b.util.Utils;
import com.pitstop.bluetooth.BluetoothDeviceManager;
import com.pitstop.bluetooth.dataPackages.DtcPackage;
import com.pitstop.bluetooth.dataPackages.FreezeFramePackage;
import com.pitstop.bluetooth.dataPackages.MultiParameterPackage;
import com.pitstop.bluetooth.dataPackages.ParameterPackage;
import com.pitstop.bluetooth.dataPackages.PidPackage;
import com.pitstop.bluetooth.dataPackages.TripInfoPackage;
import com.pitstop.models.Alarm;
import com.pitstop.models.DebugMessage;
import com.pitstop.utils.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
    public static final String MILEAGE_PARAM = "A09";
    private BluetoothCommunicator communicator;
    private BluetoothDeviceManager manager;

    ObdManager.IBluetoothDataListener dataListener;
    private Context context;
    private final String deviceName;
    private long prevIgnitionTime = -1;

    public Device215B(Context context, ObdManager.IBluetoothDataListener dataListener, String deviceName
            , long prevIgnitionTime, BluetoothDeviceManager manager) {

        this.dataListener = dataListener;
        this.context = context;
        this.deviceName = deviceName;
        this.prevIgnitionTime = prevIgnitionTime;
        this.manager = manager;
        if (this.communicator == null){
            this.communicator = new BluetoothLeComm(context, this); }
    }

    public Device215B(Context context, ObdManager.IBluetoothDataListener dataListener, String deviceName, BluetoothDeviceManager manager) {

        this.dataListener = dataListener;
        this.context = context;
        this.deviceName = deviceName;
        this.prevIgnitionTime = -1;
        this.manager = manager;
        if (this.communicator == null){
            this.communicator = new BluetoothLeComm(context, this); }
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
    public void requestData() {
        writeToObd(replyIDRPackage());
    }

    @Override
    public boolean requestSnapshot(){
        Log.d(TAG,"requestSnapshot() returning: "+pidPackage("0",0,null,"0"));
        String command =  pidPackage("0",0,null,"0");
        return writeToObd(command);
    }

    @Override
    public String getDeviceName() {
        return deviceName;
    }

    @Override
    public boolean getVin() {
        Log.d(TAG, "getVin");
        return writeToObd(qiSingle(VIN_PARAM));
    }

    public void setDeviceNameAndId(String name, String id){
        String command =  siMulti(BT_NAME_PARAM + ","  +TERMINAL_ID_PARAM
                ,name + "," + id);
        writeToObd(command);
    }

    public void setDeviceId(String id){
        String command =siSingle(TERMINAL_ID_PARAM,id);
        Log.d(TAG,"Setting device id to "+ id + ", command: "  + command );
        writeToObd(command);
    }

    @Override
    public boolean getRtc() {
        return writeToObd( qiSingle(RTC_TIME_PARAM));
    }

    @Override
    public boolean setRtc(long rtcTime) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(rtcTime);
        String dateString = new SimpleDateFormat("yyMMddHHmmss").format(calendar.getTime());
        String command =  siSingle(RTC_TIME_PARAM, dateString);
        Log.d(TAG, "settingRtcTo: " + dateString);
        return writeToObd(command);
    }

    @Override
    public boolean getPids(String pids) {
        int count = pids.split(",").length;
        String command =  pidPackage("0", count, pids, "0");
        return writeToObd(command);
    }


    @Override
    public boolean getSupportedPids() {
        String command = pidtPackage("0");
        return writeToObd(command);
    }

    @Override
    public boolean setPidsToSend(String pids, int timeInterval) {
        Log.d(TAG,"setPidsToSend: "+pids  + " time interval : " + Integer.toString(timeInterval));
        String command = siMulti(SAMPLED_PID_PARAM + "," + IDR_INTERVAL_PARAM
                ,  pids.replace(",", "/") + ","+timeInterval);
        return writeToObd(command);
    }

    @Override
    public boolean clearDtcs() {
        String command =  dtcPackage("1", "0");
        Log.d(TAG, "clearing DTC, command:  " +command);
        return writeToObd(command);
    }

    @Override
    public boolean getDtcs() {
        String command = dtcPackage("0", "0");
        Log.d(TAG, "getDtc()");
        return writeToObd(command);
    }

    @Override
    public boolean getPendingDtcs() {
        return writeToObd("");
    }

    @Override
    public boolean getFreezeFrame() {
        // 215B does not require explicit command to read FF
        return false;
    }

    // read data handler

    public String getRtcAndMileage(){
        return qiMulti(RTC_TIME_PARAM +","+ MILEAGE_PARAM);
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

    private String qiMulti(String params){
        int numberOfParams = params.split(",").length;

        String crcData = Constants.INSTRUCTION_HEAD
                + "0"
                + ","
                + Constants.INSTRUCTION_QI
                + ","
                + numberOfParams
                + ","
                + params
                +Constants.INSTRUCTION_STAR;

        String crc = Utils.toHexString(OBD.CRC(crcData));
        return crcData + crc + Constants.INSTRUCTION_FOOD;
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

    @Override
    public boolean clearDeviceMemory() {
        String command = ciSingle(Constants.CONTROL_EVENT_ID_CHD);
        Log.d(TAG, "clearing Device Memory, command:  " + command);
        return writeToObd(command);
    }
    public boolean resetDeviceToDefaults(){
        String command = ciSingle(Constants.CONTROL_EVENT_ID_RTD);
        Log.d(TAG, "resetting to defaults, command:  " +command);
        return writeToObd(command);
    }

    public boolean resetDevice(){
        String command = ciSingle(Constants.CONTROL_EVENT_ID_RESET);
        Log.d(TAG, "resetting Device, command:  " + command);
        return writeToObd(command);
    }

    private String ciSingle(String command){

        String crcData =  Constants.INSTRUCTION_HEAD +
                "0," +
                Constants.INSTRUCTION_CI +
                "," +
                command +
                "," +
                Constants.INSTRUCTION_STAR ;

        String crc = Utils.toHexString(OBD.CRC(crcData));
        return crcData + crc + Constants.INSTRUCTION_FOOD;

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

    private long lastSentTripStart = -1;
    private long lastSentTripEnd = -1;

    // parser for 215B data


    private long parseRtcTime(String rtcTime) throws ParseException {
        Log.d(TAG,"parseRtcTime() rtc: "+rtcTime);
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

    @Override
    public void onConnectionStateChange(int state) {
        this.manager.setState(state);
    }



    @Override
    public synchronized boolean connectToDevice(BluetoothDevice device) {
        if (manager.getState() == BluetoothCommunicator.CONNECTING){
            Logger.getInstance().logI(TAG,"Connecting to device: Error, already connecting/connected to a device"
                    , DebugMessage.TYPE_BLUETOOTH);
            return false;
        }
        manager.setState(BluetoothCommunicator.CONNECTING);
        dataListener.getBluetoothState(manager.getState());
        Log.i(TAG, "Connecting to LE device");
        Log.d(TAG, "connectToDevice: " + device.getName());
        ((BluetoothLeComm) communicator)
                .setReadChar(getReadChar());
        ((BluetoothLeComm) communicator)
                .setServiceUuid(getServiceUuid());
        ((BluetoothLeComm) communicator)
                .setWriteChar(getWriteChar());
        communicator.connectToDevice(device);
        return true;
    }

    @Override
    public boolean sendPassiveCommand(String payload) {
        return writeToObd(payload);
    }

    @Override
    public boolean closeConnection() {
        if (communicator == null) return false;
        communicator.close();
        return true;
    }

    @Override
    public boolean setCommunicatorState(int state) {
        if (communicator!=null){
            communicator.bluetoothStateChanged(state);
            return true;
        }
        else return false;
    }

    @Override
    public int getCommunicatorState() {
        if (communicator == null)
            return BluetoothCommunicator.DISCONNECTED;
        else {
            return communicator.getState();
        }
    }

    private boolean writeToObd(String payload) {
        Log.d(TAG,"writeToObd() payload: "+payload+ ", communicator null ? "
                +(communicator == null) + ", Connected ?  "
                +(manager.getState() == IBluetoothCommunicator.CONNECTED));

        if (communicator == null
                || manager.getState() != IBluetoothCommunicator.CONNECTED) {
            Log.d(TAG, "communicator is null or not connected.");
            return false;
        }

        if (payload == null || payload.isEmpty()) {
            return false;
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

            bytes = getBytes(data);


            if (bytes == null || bytes.length == 0) {
                return false;
            }
            if (!communicator.writeData(bytes)){
                return false;
            }
        }
        return true;
    }

    @Override
    public void parseData(byte[] data) {

        String readData = "";
        try {
            readData = new String(data, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        Log.v(TAG, "Data Read: " + readData.replace("\r", "\\r").replace("\n", "\\n"));
        if(readData.isEmpty()) {
            return;
        }

        try {
            parseReadData(readData);
        } catch (Exception e){
            sbRead = new StringBuilder(); // reset sb
            e.printStackTrace();
        }
    }

    private void parseReadData(String msg) throws Exception{
        sbRead.append(msg);

        if (sbRead.toString().contains("\r\n")) {
            String msgInfo = sbRead.toString().replace("\r\n", "\\r\\n");
            msgInfo = msgInfo.substring(msgInfo.lastIndexOf("$$"), msgInfo.length() - 1); // TODO: 16/12/13 Test this

            Log.v(TAG, "Data Read: " + msgInfo);

            sbRead = new StringBuilder();

            // determine response type
            if (Constants.INSTRUCTION_IDR.equals(DataParseUtil
                    .parseMsgType(msgInfo))) {

                String dateStr = DateUtil.getSystemTime("yyyy-MM-dd HH:mm:ss");

                IDRInfo idrInfo = DataParseUtil.parseIDR(msgInfo);
                idrInfo.time = dateStr;
                try{
                    Log.d(TAG, idrInfo.terminalSN);
                    dataListener.idrFuelEvent(idrInfo.terminalSN, Double.valueOf(idrInfo.fuelConsumption));
                    Log.d(TAG, "fuelCOnsumedUpdate: " + Double.valueOf(idrInfo.fuelConsumption));
                }
                catch (NumberFormatException e){
                    Log.d(TAG, "idrInfo fuel consumption numberFormatException");

                }
                long ignitionTime; // ignition time parsed as unix time seconds
                try {
                    ignitionTime = Long.parseLong(idrInfo.ignitionTime);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    ignitionTime = 0;
                }


                boolean ignitionTimeChanged = false;

                if (prevIgnitionTime != ignitionTime){
                    ignitionTimeChanged = true;
                    prevIgnitionTime = ignitionTime;
                }

                // Trip end/start
                if(idrInfo.mileage != null && !idrInfo.mileage.isEmpty()) {

                    TripInfoPackage tripInfoPackage = new TripInfoPackage();
                    tripInfoPackage.deviceId = idrInfo.terminalSN;
                    tripInfoPackage.rtcTime = parseRtcTime(String.valueOf(ignitionTime))
                            + Long.parseLong(idrInfo.runTime);
                    tripInfoPackage.tripId = ignitionTime;
                    Log.d(TAG,"tripInfoPackage.tripId = "+tripInfoPackage.tripId
                            +" rtcTime = "+tripInfoPackage.rtcTime +" runTime: "+idrInfo.runTime);

                    Logger.getInstance().logD(TAG, "IDR_INFO TRIP, alarmEvent: "+idrInfo.alarmEvents
                            +", ignitionTimeChanged?"+ignitionTimeChanged +", deviceId: "
                            +idrInfo.terminalSN, DebugMessage.TYPE_BLUETOOTH);

                    if (idrInfo.alarmEvents.equals("2")){
                        tripInfoPackage.flag = TripInfoPackage.TripFlag.END;
                    }
                    /*Trip start detected by ignition time changing or alarm, if both occur
                    /* , one will be sent as an update*/
                    else if (idrInfo.alarmEvents.equals("1") || ignitionTimeChanged){
                        tripInfoPackage.flag = TripInfoPackage.TripFlag.START;
                    }
                    else{
                        tripInfoPackage.flag = TripInfoPackage.TripFlag.UPDATE;
                    }
                    if (idrInfo.alarmEvents != null && !idrInfo.alarmEvents.isEmpty()){
                        Float alarmValue;
                        if (idrInfo.alarmValues == null|| idrInfo.alarmValues.equalsIgnoreCase("")){
                            alarmValue =(float)0;
                        }
                        else {
                            alarmValue = Float.valueOf(idrInfo.alarmValues);
                        }
                        dataListener.alarmEvent(new Alarm(Integer.valueOf(idrInfo.alarmEvents),
                                alarmValue,
                                String.valueOf(Long.valueOf(idrInfo.runTime) +parseRtcTime(Long.toString(ignitionTime)))
                                , null));
                    }
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
                    if (idrInfo.ignitionTime != null)
                        pidPackage.tripId = idrInfo.ignitionTime;
                    else pidPackage.tripId = "";

                    dataListener.idrPidData(pidPackage);
                }
                else{
                    dataListener.idrPidData(null);
                }

                if(idrInfo.dtc != null && !idrInfo.dtc.isEmpty()) {
                    // dtc example: "0/07470107/07470207/07470307/07474307"

                    String[] unparsedDtcCodes = idrInfo.dtc.split("/");

                    if(unparsedDtcCodes.length > 1) {
                        Log.i(TAG, "Parsing DTC data: " + idrInfo.dtc);

                        DtcPackage dtcPackage = new DtcPackage();

                        boolean isPending = idrInfo.dtc.charAt(0) == '1';

                        // first element is dtc type
                        for(int i = 1 ; i < unparsedDtcCodes.length ; i++) {
                            unparsedDtcCodes[i] = unparsedDtcCodes[i].substring(4);
                        }

                        List<FaultInfo> faultInfo = FaultParse.parse(context, unparsedDtcCodes);

                        dtcPackage.dtcs = new HashMap<>();

                        for(int i = 1 ; i < faultInfo.size() ; i++) {
                            dtcPackage.dtcs.put(faultInfo.get(i).code,isPending);
                        }

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
                    Log.d(TAG,"SnapShot"+ idrInfo.snapshot);
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

                //multiple params
                if (settingInfo.terminalRTCTime != null && settingInfo.totalMileage != null){
                    MultiParameterPackage parameterPackage = new MultiParameterPackage();
                    parameterPackage.success = true;
                    parameterPackage.deviceId = settingInfo.terminalSN;
                    parameterPackage.mParamsValueMap.put(ParameterPackage.ParamType.RTC_TIME, String.valueOf(parseRtcTime(settingInfo.terminalRTCTime)));
                    String mileageKM = String.valueOf(Double.valueOf(settingInfo.totalMileage) / 1000);
                    parameterPackage.mParamsValueMap.put(ParameterPackage.ParamType.MILEAGE, mileageKM);
                    dataListener.parameterData(parameterPackage);

                } else {
                    // assumes only one parameter queried per command
                    ParameterPackage parameterPackage = new ParameterPackage();
                    parameterPackage.deviceId = settingInfo.terminalSN;
                    if (settingInfo.terminalRTCTime != null) {
                        try {
                            long rtcTime = parseRtcTime(settingInfo.terminalRTCTime);
                            parameterPackage.success = true;
                            parameterPackage.paramType = ParameterPackage.ParamType.RTC_TIME;
                            parameterPackage.value = String.valueOf(rtcTime);
                        } catch (ParseException e) {
                            e.printStackTrace();
                            parameterPackage.success = false;
                        }
                    } else if (settingInfo.vehicleVINCode != null) {
                        parameterPackage.success = true;
                        parameterPackage.paramType = ParameterPackage.ParamType.VIN;
                        parameterPackage.value = settingInfo.vehicleVINCode;
                    } else {
                        parameterPackage.success = false;
                    }

                    dataListener.parameterData(parameterPackage);
                }
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

                boolean isPending = dtcInfo.dtcType == 1;

                // dtc example:
                // $$HT-IDD215B-S00V002,DTC,1,04,4,07470107,07470207,07470307,07474307,*308F\r\n
                // DTCInfo [deviceId=HT-IDD215B-S00V002, dtcType=1, diagnosisProtocol=04, dtcNumber=4, dtcs=[07470107, 07470207, 07470307, 07474307]]

                String[] unparsedDtcCodes = new String[dtcInfo.dtcs != null ? dtcInfo.dtcs.length : 0];

                for(int i = 0 ; i < unparsedDtcCodes.length ; i++) {
                    unparsedDtcCodes[i] = dtcInfo.dtcs[i].substring(4);
                }

                List<FaultInfo> faultInfo = FaultParse.parse(context, unparsedDtcCodes);

                dtcPackage.dtcs = new HashMap<>();

                for(int i = 0 ; i < faultInfo.size() ; i++) {
                    dtcPackage.dtcs.put(faultInfo.get(i).code,isPending);
                }

                dtcPackage.rtcTime = String.valueOf(System.currentTimeMillis() / 1000);

                dtcPackage.deviceId = dtcInfo.terminalId;

                dataListener.dtcData(dtcPackage);
            }
        }
    }






}
