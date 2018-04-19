package com.pitstop;

import com.pitstop.bluetooth.dataPackages.ELM327PidPackage;
import com.pitstop.bluetooth.dataPackages.OBD212PidPackage;
import com.pitstop.bluetooth.dataPackages.OBD215PidPackage;
import com.pitstop.models.sensor_data.SensorData;
import com.pitstop.utils.SensorDataUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Created by Karol Zdebel on 4/19/2018.
 */

public class SensorDataTestUtil {

    private static final String HEX_VAL = "0123456789ABCDEF";

    public static Collection<SensorData> get215SensorData(int len, String deviceId, String vin){
        return SensorDataUtils.Companion.pidCollectionToSensorDataCollection(get212PidData(len,deviceId),vin);
    }

    public static List<OBD215PidPackage> get215PidData(int len, String deviceID){
        List<OBD215PidPackage> obd215PidPackageList = new ArrayList<>();
        for (int i=0;i<len;i++){
            OBD215PidPackage obd215PidPackage = new OBD215PidPackage(deviceID
                    , String.valueOf(System.currentTimeMillis()+(i*10000L))
                    ,"0",System.currentTimeMillis()+(i*10000L));
            obd215PidPackage.setPids(getPidMap());
            obd215PidPackageList.add(obd215PidPackage);
        }
        return obd215PidPackageList;
    }

    public static List<OBD212PidPackage> get212PidData(int len, String deviceID){
        List<OBD212PidPackage> obd212PidPackageList = new ArrayList<>();
        for (int i=0;i<len;i++){
            OBD212PidPackage obd212PidPackage = new OBD212PidPackage(deviceID
                    , String.valueOf(System.currentTimeMillis()+(i*10000L))
                    ,"0",System.currentTimeMillis()+(i*10000L));
            obd212PidPackage.setPids(getPidMap());
            obd212PidPackageList.add(obd212PidPackage);
        }
        return obd212PidPackageList;
    }

    public static List<ELM327PidPackage> get327PidData(int len, String deviceID){
        List<ELM327PidPackage> elm327PidPackageList = new ArrayList<>();
        for (int i=0;i<len;i++){
            ELM327PidPackage elm327PidPackage = new ELM327PidPackage(deviceID
                    ,System.currentTimeMillis()+(i*10000L));
            elm327PidPackage.setPids(getPidMap());
            elm327PidPackageList.add(elm327PidPackage);
        }
        return elm327PidPackageList;
    }

    private static Map<String,String> getPidMap(){
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

    private static String getRandomPidValue(int len){
        StringBuffer buffer = new StringBuffer();
        Random random = new Random();
        for (int i=0;i<len;i++){
            int index = (int)(HEX_VAL.length()*random.nextFloat());
            buffer.append(HEX_VAL.charAt(index));
        }
        return buffer.toString();
    }
}
