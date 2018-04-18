package com.pitstop.retrofit;

import android.util.Log;

import com.pitstop.bluetooth.dataPackages.ELM327PidPackage;
import com.pitstop.bluetooth.dataPackages.OBD212PidPackage;
import com.pitstop.bluetooth.dataPackages.OBD215PidPackage;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Created by Karol Zdebel on 4/18/2018.
 */

public class PitstopSensorDataApiTest {

    private final String TAG = PitstopSensorDataApiTest.class.getSimpleName();

    @Test
    public void storeSensorDataTest(){
        Log.d(TAG,"storeSensorDataTest()");

    }

    private List<OBD215PidPackage> get215PidData(String deviceID, String VIN, int len){
        List<OBD215PidPackage> obd215PidPackageList = new ArrayList<>();
        for (int i=0;i<len;i++){
            //OBD215PidPackage obd215PidPackage = new OBD215PidPackage(deviceID,  )
        }
    }

    private List<OBD212PidPackage> get212PidData(){

    }

    private List<ELM327PidPackage> get327PidData(){

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
