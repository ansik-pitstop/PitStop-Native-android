package com.castel.obd.bleDevice;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.castel.obd.OBD;
import com.castel.obd.bluetooth.ObdManager;
import com.castel.obd.info.DataPackageInfo;
import com.castel.obd.info.ParameterInfo;
import com.castel.obd.info.ParameterPackageInfo;
import com.castel.obd.util.Utils;
import com.castel.obd215b.info.DTCInfo;
import com.castel.obd215b.info.IDRInfo;
import com.castel.obd215b.info.SettingInfo;
import com.castel.obd215b.util.Constants;
import com.castel.obd215b.util.DataPackageUtil;
import com.castel.obd215b.util.DataParseUtil;
import com.castel.obd215b.util.DateUtil;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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


    ObdManager.IBluetoothDataListener dataListener;
    private Context context;

    public Device215B(Context context, ObdManager.IBluetoothDataListener dataListener) {
        this.dataListener = dataListener;
        this.context = context;
    }

    // functions

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
    public String getSupportedPids() {
        return pidtPackage("0");
    }

    @Override
    public String setPidsToSend(String pids) {
        return siMulti("A14,A15,A18",  pids.replace(",", "/") + "10,1");
    }

    @Override
    public String getDtcs() {
        return dtcPackage("0", "0");
    }

    // read data handler

    @Override
    public void onCharacteristicChanged(BluetoothGattCharacteristic characteristic) {
        final byte[] data = characteristic.getValue();

        String readData = "";

        try {
            readData = new String(data, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        Log.d(TAG, "Data Read: " + readData);

        if(readData.isEmpty()) {
            return;
        }

        final String dataToParse = readData;

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                parseReadData(dataToParse);
            }
        });
    }

    @Override
    public void onCharacteristicRead(BluetoothGattCharacteristic characteristic, int status) {
        if(status != BluetoothGatt.GATT_SUCCESS) {
            return;
        }

        onCharacteristicChanged(characteristic);
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

        String crc = com.castel.obd215b.util.Utils.toHexString(OBD.CRC(crcData));

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

        String crc = com.castel.obd215b.util.Utils.toHexString(OBD.CRC(crcData));

        String msg = crcData + crc + Constants.INSTRUCTION_FOOD;

        return msg;
    }

    private String siMulti(String params, String values) {
        int numberOfParams = params.split(",").length;

        if (values.split(",").length != numberOfParams) {
            Log.w("siMulti", "Number of params and values must match");
            return "";
        }

        String crcData = Constants.INSTRUCTION_HEAD
                + "0"
                + ","
                + Constants.INSTRUCTION_SI
                + ","
                + numberOfParams
                + ","
                + params
                + ","
                + values
                + ","
                + Constants.INSTRUCTION_STAR;

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

            sbRead = new StringBuilder();
            if (Constants.INSTRUCTION_IDR.equals(DataParseUtil
                    .parseMsgType(msgInfo))) {

                String dateStr = DateUtil.getSystemTime("yyyy-MM-dd HH:mm:ss");

                IDRInfo idrInfo = DataParseUtil.parseIDR(msgInfo);
                idrInfo.time = dateStr;

                //Log.i(TAG, idrInfo.toString());

            } else if (Constants.INSTRUCTION_CI
                    .equals(DataParseUtil.parseMsgType(msgInfo))) {
                //boolean bResult = DataParseUtil
                //        .parseSetting(msgInfo);
//
                //intent.putExtra(EXTRA_DATA_TYPE,
                //        Constants.INSTRUCTION_CI);
                //intent.putExtra(EXTRA_DATA, bResult);
                //LocalBroadcastManager.getInstance(this)
                //        .sendBroadcast(intent);
//
                ////
                //broadcastContent(ACTION_COMMAND_TEST,
                //        COMMAND_TEST_WRITE, getResources().getString(R.string.report_data) + msgInfo + "\n");
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

                ParameterPackageInfo parameterPackageInfo = new ParameterPackageInfo();
                parameterPackageInfo.deviceId = settingInfo.terminalSN;
                parameterPackageInfo.result = 1;
                parameterPackageInfo.value = new ArrayList<>();

                if(settingInfo.terminalRTCTime != null) {
                    try {
                        long rtcTime = new SimpleDateFormat("yyMMddHHmmss").parse(settingInfo.terminalRTCTime).getTime() / 1000;
                        parameterPackageInfo.value.add(new ParameterInfo(DataPackageUtil.RTC_TIME_PARAM, String.valueOf(rtcTime)));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
                if(settingInfo.vehicleVINCode != null) {
                    parameterPackageInfo.value.add(new ParameterInfo(ObdManager.VIN_TAG, settingInfo.vehicleVINCode));
                }

                dataListener.getParameterData(parameterPackageInfo);
            } else if (Constants.INSTRUCTION_PIDT
                    .equals(DataParseUtil.parseMsgType(msgInfo))) {
                // PIDInfo pidInfo = DataParseUtil.parsePIDT(msgInfo);
//
                // intent.putExtra(EXTRA_DATA_TYPE,
                //         Constants.INSTRUCTION_PIDT);
                // intent.putExtra(EXTRA_DATA, pidInfo);
                // LocalBroadcastManager.getInstance(this)
                //         .sendBroadcast(intent);
//
                // broadcastContent(ACTION_COMMAND_TEST,
                //         COMMAND_TEST_WRITE, getResources().getString(R.string.report_data) + msgInfo + "\n");
            } else if (Constants.INSTRUCTION_PID
                    .equals(DataParseUtil.parseMsgType(msgInfo))) {
                //PIDInfo pidInfo = DataParseUtil.parsePID(msgInfo);
//
                //intent.putExtra(EXTRA_DATA_TYPE,
                //        Constants.INSTRUCTION_PID);
                //intent.putExtra(EXTRA_DATA, pidInfo);
                //LocalBroadcastManager.getInstance(this)
                //        .sendBroadcast(intent);
//
                //broadcastContent(ACTION_COMMAND_TEST,
                //        COMMAND_TEST_WRITE, getResources().getString(R.string.report_data) + msgInfo + "\n");
            } else if (Constants.INSTRUCTION_DTC
                    .equals(DataParseUtil.parseMsgType(msgInfo))) {
                Log.i(TAG, msgInfo);
                DTCInfo dtcInfo = DataParseUtil.parseDTC(msgInfo);

                Log.i(TAG, dtcInfo.toString());

                DataPackageInfo dataPackageInfo = new DataPackageInfo();

                dataPackageInfo.result = 6;
                StringBuilder sb = new StringBuilder();
                if(dtcInfo.dtcs != null) {
                    for (String dtc : dtcInfo.dtcs) {
                        sb.append(dtc.substring(4));
                        sb.append(",");
                    }
                }
                if(sb.length() > 0) {
                    sb.deleteCharAt(sb.length() - 1);
                }
                dataPackageInfo.dtcData = sb.toString();
                dataPackageInfo.deviceId = dtcInfo.terminalId;
                dataPackageInfo.rtcTime = String.valueOf(System.currentTimeMillis() / 1000);
                dataListener.getIOData(dataPackageInfo);
            } else if (Constants.INSTRUCTION_OTA
                    .equals(DataParseUtil.parseMsgType(msgInfo))) {
                //LogUtil.i("--????OTA????--:" + msgInfo);
//
                //// ???????
                //mRecieveTerminate = System.currentTimeMillis();
                //LogUtil.v("send next data2");
                //broadcastUpdateContent(ACTION_PACKAGE_CONTENT,
                //        "\n"+getResources().getString(R.string.report_data) + msgInfo+"\n"
                //                + DateUtil.getSystemTime()+"\n");
//
                //intent.putExtra(EXTRA_DATA_TYPE,
                //        Constants.INSTRUCTION_OTA);
                //intent.putExtra(EXTRA_DATA,
                //        DataParseUtil.parseOTA(msgInfo));
                //intent.putExtra(EXTRA_DATA1, DataParseUtil.parseUpgradeType(msgInfo));
//
                //LocalBroadcastManager.getInstance(this)
                //        .sendBroadcast(intent);

            } else if (Constants.INSTRUCTION_TEST
                    .equals(DataParseUtil.parseMsgType(msgInfo))) {
                //LogUtil.i("--??????--:" + msgInfo);
                //broadcastContent(ACTION_HARDWARE_TEST,
                //        HARDWARE_TEST_DATA, "\n"+getResources().getString(R.string.report_data) + msgInfo);
            }

            if (!Constants.INSTRUCTION_TEST.equals(DataParseUtil
                    .parseMsgType(msgInfo))) {
            }
        }
    }

}
