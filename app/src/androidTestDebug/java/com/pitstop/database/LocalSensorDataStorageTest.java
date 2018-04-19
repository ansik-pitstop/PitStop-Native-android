package com.pitstop.database;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.util.Log;

import com.google.gson.Gson;
import com.pitstop.SensorDataTestUtil;
import com.pitstop.models.sensor_data.SensorData;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import java.util.Collection;

/**
 * Created by Karol Zdebel on 4/19/2018.
 */

public class LocalSensorDataStorageTest {
    private final String TAG = LocalSensorDataStorageTest.class.getSimpleName();
    private final String VIN = "1GB0CVCL7BF147611";
    private String deviceID = "215B002373";

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
        Collection<SensorData> sensorDataCollection
                =  SensorDataTestUtil.get215SensorData(1,deviceID,VIN);
        Log.d(TAG,"generated sensor data: "+gson.toJsonTree(sensorDataCollection));
        sensorDataCollection.forEach(sensorData -> localSensorDataStorage.store(sensorData));
        Collection<SensorData> retrievedSensorDataCollection = localSensorDataStorage.getAll();
        Log.d(TAG,"retrieved: "+gson.toJsonTree(retrievedSensorDataCollection));
        Assert.assertEquals(sensorDataCollection,retrievedSensorDataCollection);
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
