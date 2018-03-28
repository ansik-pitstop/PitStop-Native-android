package com.pitstop.database;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.google.gson.Gson;
import com.pitstop.Util;
import com.pitstop.models.trip.TripData;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * Created by Karol Zdebel on 3/20/2018.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class LocalPendingTripStorageTest {

    private final String TAG = LocalPendingTripStorageTest.class.getSimpleName();
    private final String VIN = "1GB0CVCL7BF147611";

    private LocalPendingTripStorage localPendingTripStorage;
    private Gson gson;

    @Before
    public void setup(){
        Context context = InstrumentationRegistry.getTargetContext();
        localPendingTripStorage = new LocalPendingTripStorage(context);
        localPendingTripStorage.deleteAll();
        gson = new Gson();
    }

    @Test
    public void storePendingTripTest(){
        Log.d(TAG,"running storePendingTripTest()");
        int locNum = 3;
        localPendingTripStorage.deleteAll();
        TripData tripData = Util.Companion.generateTripData(true,locNum,VIN,System.currentTimeMillis());
        Log.d(TAG,"storePendingTripTest() tripData = "+gson.toJsonTree(tripData));
        assertTrue(localPendingTripStorage.store(tripData) > 0);
        int rows = localPendingTripStorage.deleteIncomplete();
        Log.d(TAG,"deleteIncomplete() response: "+rows);
        List<TripData> tripDataRetrieved = localPendingTripStorage.getCompleted();
        Log.d(TAG,"tripData after retrieving: "+gson.toJsonTree(tripDataRetrieved));
        assertEquals(tripDataRetrieved.size(),1);
        assertEquals(tripData,tripDataRetrieved.get(0));

    }

    @Test
    public void getMultipleTripTest(){
        List<TripData> tripDataList = new ArrayList<>();
        localPendingTripStorage.deleteAll();
        for (int i=0;i<3;i++){
            TripData tripData = Util.Companion.generateTripData(true,3,VIN,System.currentTimeMillis());
            tripDataList.add(tripData);
            localPendingTripStorage.store(tripData);
        }

        localPendingTripStorage.deleteIncomplete();
        List<TripData> tripDataRetrieved = localPendingTripStorage.getCompleted();

        Log.d(TAG,"tripDataRetrieved: "+gson.toJsonTree(tripDataRetrieved));
        Log.d(TAG,"tripDataStored: "+gson.toJsonTree(tripDataList));

        Set<TripData> t1 = new HashSet<>(tripDataRetrieved);
        Set<TripData> t2 = new HashSet<>(tripDataList);
        assertEquals(t1, t2);
    }

    @Test
    public void completeTripsTest(){
        Log.d(TAG,"completeTripsTest()");
        int locNum = 3;
        localPendingTripStorage.deleteAll();
        TripData tripData = Util.Companion.generateTripData(false
                ,locNum,VIN,System.currentTimeMillis());
        TripData tripData2 = Util.Companion.generateTripData(true
                ,locNum,VIN,System.currentTimeMillis());
        localPendingTripStorage.store(tripData);

        //Make sure incomplete data is being removed
        assertEquals(0,localPendingTripStorage.getCompleted().size());
        assertEquals(0,localPendingTripStorage.deleteIncomplete());
        assertTrue(localPendingTripStorage.completeAll() > 0);
        assertTrue(localPendingTripStorage.deleteIncomplete() > 0);
        assertEquals(0,localPendingTripStorage.getCompleted().size());

        //Make sure completed data isn't being removed by deleteIncomplete()
        localPendingTripStorage.store(tripData);
        localPendingTripStorage.store(tripData2);
        assertTrue(localPendingTripStorage.deleteIncomplete() > 0);
        List<TripData> retrieved = localPendingTripStorage.getCompleted();
        assertEquals(1,retrieved.size());
        assertEquals(retrieved.get(0),tripData2);

    }
}
