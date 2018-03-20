package com.pitstop.database;

import android.content.Context;
import android.location.Geocoder;
import android.location.Location;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.pitstop.models.trip.TripData;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static junit.framework.Assert.assertEquals;

/**
 * Created by Karol Zdebel on 3/20/2018.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class LocalPendingTripStorageTest {

    private final String TAG = LocalPendingTripStorageTest.class.getSimpleName();
    private final int LOC_NUM = 2;
    private final String VIN = "1GB0CVCL7BF147611";

    private LocalPendingTripStorage localPendingTripStorage;
    private Geocoder geocoder;
    private TripData tripData;


    @Before
    public void setup(){
        Context context = InstrumentationRegistry.getTargetContext();
        localPendingTripStorage = new LocalPendingTripStorage(context);
        localPendingTripStorage.deleteAll();
        geocoder = new Geocoder(context);

        List<Location> testData = new ArrayList<>();
        for (int i=0;i<LOC_NUM;i++){
            testData.add(getRandomLocation());
        }
        tripData = Util.Companion.locationsToDataPoints(VIN,testData);

    }

    @Test
    public void storePendingTripTest(){
        Log.d(TAG,"running storePendingTripTest(), tripData = "+tripData);
        localPendingTripStorage.store(tripData);
        List<TripData> tripDataRetrieved = localPendingTripStorage.get();
        Log.d(TAG,"tripData after retrieving: "+tripData);
        assertEquals(tripDataRetrieved.size(),1);
        assertEquals(tripData.getId(),tripDataRetrieved.get(0).getId());
        assertEquals(tripData.getLocations().size(),tripDataRetrieved.get(0).getLocations().size());

    }

    private Location getRandomLocation(){
        Random r = new Random();
        Location location = new Location("dummyprovider");
        location.setLatitude(r.nextDouble()*90);
        location.setLongitude(r.nextDouble()*90);
        location.setTime(System.currentTimeMillis());
        Log.d(TAG,"time = "+Math.abs(r.nextInt()*1000000));
        return location;
    }
}
