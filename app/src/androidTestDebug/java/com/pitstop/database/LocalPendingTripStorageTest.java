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
import java.util.List;

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
        TripData tripData = Util.Companion.generateTripData(locNum,VIN,System.currentTimeMillis());
        Log.d(TAG,"storePendingTripTest() tripData = "+tripData);
        assertTrue(localPendingTripStorage.store(tripData) > 0);
        List<TripData> tripDataRetrieved = localPendingTripStorage.get();
        Log.d(TAG,"tripData after retrieving: "+gson.toJsonTree(tripDataRetrieved));
        assertEquals(tripDataRetrieved.size(),1);
        assertTrue(Util.Companion.checkTripsEqual(tripData,tripDataRetrieved.iterator().next()));

    }

    @Test
    public void getMultipleTripTest(){
        List<TripData> tripDataList = new ArrayList<>();
        for (int i=0;i<3;i++){
            TripData tripData = Util.Companion.generateTripData(3,VIN,System.currentTimeMillis());
            tripDataList.add(tripData);
            localPendingTripStorage.store(tripData);
        }

        List<TripData> tripDataRetrieved = localPendingTripStorage.get();

        assertEquals(tripDataList,tripDataRetrieved);
    }
}
