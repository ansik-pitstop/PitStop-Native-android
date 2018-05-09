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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * Created by Karol Zdebel on 4/19/2018.
 */

public class LocalSensorDataStorageTest {
    private final String TAG = LocalSensorDataStorageTest.class.getSimpleName();
    private final String VIN = "1GB0CVCL7BF147611";
    private final String deviceID = "215B002373";
    private final String VIN2 = "1BK0FUC7BF147Y0U";
    private final String deviceID2 = "215B002888";

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
    public void storeGetAllSensorDataTest(){
        Log.d(TAG,"storeSensorDataTest()");
        Collection<SensorData> sensorDataCollection
                =  SensorDataTestUtil.get215SensorData(3,deviceID,VIN,0);
        sensorDataCollection.addAll(SensorDataTestUtil.get215SensorData(2,deviceID2,VIN2,3));
        Log.d(TAG,"generated sensor data: "+gson.toJsonTree(sensorDataCollection));
        sensorDataCollection.forEach(sensorData -> localSensorDataStorage.store(sensorData));
        Collection<SensorData> retrievedSensorDataCollection = localSensorDataStorage.getAll();
        Log.d(TAG,"retrieved: "+gson.toJsonTree(retrievedSensorDataCollection));
        Assert.assertEquals(sensorDataCollection,retrievedSensorDataCollection);
        localSensorDataStorage.deleteAll();
    }

    @Test
    public void deleteSensorDataTest(){
        Log.d(TAG,"deleteSensorDataTest()");
        List<SensorData> sensorDataCollection
                =  new ArrayList<>(SensorDataTestUtil.get215SensorData(3,deviceID,VIN,0));
        sensorDataCollection.addAll(SensorDataTestUtil.get215SensorData(2,deviceID2,VIN2,3));

        sensorDataCollection.forEach((next) -> {
            Log.d(TAG,"rtcTime: "+next.getBluetoothDeviceTime());
            localSensorDataStorage.store(next);
        });
        List<SensorData> toRemove = new ArrayList<>();

        //Remove first and last from db and compare
        toRemove.add(sensorDataCollection.get(0));
        toRemove.add(sensorDataCollection.get(sensorDataCollection.size()-1));
        sensorDataCollection.remove(0);
        sensorDataCollection.remove(sensorDataCollection.size()-1);
        Assert.assertTrue(localSensorDataStorage.delete(toRemove) > 0);
        Assert.assertEquals(new HashSet<>(sensorDataCollection),new HashSet<>(localSensorDataStorage.getAll()));

        //Remove remaining from db and make sure its empty afterwards
        Assert.assertTrue(localSensorDataStorage.delete(sensorDataCollection) > 0);
        Assert.assertTrue(localSensorDataStorage.getAll().isEmpty());
        localSensorDataStorage.deleteAll();
    }

    @Test
    public void deleteAllSensorDataTest(){
        Log.d(TAG,"deleteSensorDataTest()");
    }
}
