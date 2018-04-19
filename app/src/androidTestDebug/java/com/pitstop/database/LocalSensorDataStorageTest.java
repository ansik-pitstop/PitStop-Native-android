package com.pitstop.database;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.util.Log;

import com.google.gson.Gson;

import org.junit.Before;
import org.junit.Test;

/**
 * Created by Karol Zdebel on 4/19/2018.
 */

public class LocalSensorDataStorageTest {
    private final String TAG = LocalSensorDataStorageTest.class.getSimpleName();
    private final String VIN = "1GB0CVCL7BF147611";

    private LocalSensorDataStorage localSensorDataStorage;
    private Gson gson;

    @Before
    public void setup() {
        Context context = InstrumentationRegistry.getTargetContext();
        localSensorDataStorage = new LocalSensorDataStorage(context);
        localSensorDataStorage.deleteAll();
        gson = new Gson();
    }

    @Test
    public void storeSensorDataTest(){
        Log.d(TAG,"storeSensorDataTest()");
    }

    @Test
    public void getAllSensorDataTest(){
        Log.d(TAG,"getAllSensorDataTest");
    }

    @Test
    public void deleteAllSensorDataTest(){
        Log.d(TAG,"deleteSensorDataTest()");
    }
}
