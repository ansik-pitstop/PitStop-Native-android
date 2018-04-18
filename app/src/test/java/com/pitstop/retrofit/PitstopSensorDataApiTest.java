package com.pitstop.retrofit;

import com.google.gson.Gson;
import com.pitstop.bluetooth.dataPackages.ELM327PidPackage;
import com.pitstop.bluetooth.dataPackages.OBD212PidPackage;
import com.pitstop.bluetooth.dataPackages.OBD215PidPackage;
import com.pitstop.models.sensor_data.pid.PidData;
import com.pitstop.utils.SensorDataUtils;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Created by Karol Zdebel on 4/18/2018.
 */

public class PitstopSensorDataApiTest {

    private final String TAG = PitstopSensorDataApiTest.class.getSimpleName();

    @Test
    public void storeSensorDataTest(){
        System.out.println("storeSensorDataTest()");
        String VIN = "1G1JC5444R7252367";
        String deviceID = "215B002373";
        int pidNum = 1;

        Set<PidData> inputData = SensorDataUtils.Companion
                .pidListToSensorDataFormat(get215PidData(pidNum,deviceID),VIN);
        System.out.println("Input data: "+new Gson().toJsonTree(inputData));
    }

    private List<OBD215PidPackage> get215PidData(int len, String deviceID){
        List<OBD215PidPackage> obd215PidPackageList = new ArrayList<>();
        for (int i=0;i<len;i++){
            OBD215PidPackage obd215PidPackage = new OBD215PidPackage(deviceID
                    , String.valueOf(System.currentTimeMillis()),"0");
            obd215PidPackage.setPids(getPidMap());
            obd215PidPackageList.add(obd215PidPackage);
        }
        return obd215PidPackageList;
    }

    private List<OBD212PidPackage> get212PidData(int len, String deviceID){
        List<OBD212PidPackage> obd212PidPackageList = new ArrayList<>();
        for (int i=0;i<len;i++){
            OBD212PidPackage obd212PidPackage = new OBD212PidPackage(deviceID
                    , String.valueOf(System.currentTimeMillis()),"0");
            obd212PidPackage.setPids(getPidMap());
            obd212PidPackageList.add(obd212PidPackage);
        }
        return obd212PidPackageList;
    }

    private List<ELM327PidPackage> get327PidData(int len, String deviceID){
        List<ELM327PidPackage> elm327PidPackageList = new ArrayList<>();
        for (int i=0;i<len;i++){
            ELM327PidPackage elm327PidPackage = new ELM327PidPackage(deviceID);
            elm327PidPackage.setPids(getPidMap());
            elm327PidPackageList.add(elm327PidPackage);
        }
        return elm327PidPackageList;
    }

    private Map<String,String> getPidMap(){
        Map<String,String> pids = new HashMap<>();
        pids.put("2105",getRandomPidValue(2));
        pids.put("2106",getRandomPidValue(2));
        pids.put("2107",getRandomPidValue(2));
        pids.put("210B",getRandomPidValue(2));
        pids.put("210C",getRandomPidValue(4));
        pids.put("210D",getRandomPidValue(2));
        pids.put("210E",getRandomPidValue(2));
        pids.put("210F",getRandomPidValue(2));
        pids.put("2110",getRandomPidValue(4));
        pids.put("2142",getRandomPidValue(4));
        return pids;
    }

    private final String HEX_VAL = "0123456789ABCDEF";

    private String getRandomPidValue(int len){
        StringBuffer buffer = new StringBuffer();
        Random random = new Random();
        for (int i=0;i<len;i++){
            int index = (int)(HEX_VAL.length()*random.nextFloat());
            buffer.append(HEX_VAL.charAt(index));
        }
        return buffer.toString();
    }

}
