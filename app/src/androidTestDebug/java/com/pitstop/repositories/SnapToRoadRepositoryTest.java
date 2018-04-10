package com.pitstop.repositories;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.pitstop.models.snapToRoad.SnappedPoint;
import com.pitstop.retrofit.GoogleSnapToRoadApi;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static junit.framework.Assert.assertTrue;

/**
 * Created by David C. on 23/3/18.
 */

@RunWith(AndroidJUnit4.class)
@LargeTest
public class SnapToRoadRepositoryTest {

    private final String TAG = AppointmentRepositoryTest.class.getSimpleName();
    private SnapToRoadRepository snapToRoadRepository;

    @Before
    public void setup() {
        Log.i(TAG, "running setup()");
        Context context = InstrumentationRegistry.getTargetContext();
        GoogleSnapToRoadApi api = RetrofitTestUtil.Companion.getSnapToRoadApi();

        snapToRoadRepository = new SnapToRoadRepository(api);
    }

    @Test
    public void getSnapToRoadFromLocationsTest() {

        Log.i(TAG, "running getTripsByCarVinTest()");

        //Input
        CompletableFuture<List<SnappedPoint>> future = new CompletableFuture<>();

        String latLngList = "43.6194466400637,-79.5550531196223|43.6194466400637,-79.5550531196223|43.6194466400637,-79.5550531196223|43.6190741183642,-79.5537706185873|43.6190741183642,-79.5485321805622";

        snapToRoadRepository.getSnapToRoadFromLocations(latLngList)
                .doOnNext(next -> {

                    Log.i(TAG, "Got appointments: " + next);
                    future.complete(next.getData());

                }).subscribe();

        try {
            assertTrue(future
                    .get(2000, java.util.concurrent.TimeUnit.MILLISECONDS).size() > 0);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }

    }
}
