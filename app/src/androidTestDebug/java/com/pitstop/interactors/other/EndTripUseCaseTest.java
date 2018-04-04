package com.pitstop.interactors.other;

import android.content.Context;
import android.location.Location;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.pitstop.Util;
import com.pitstop.database.LocalPendingTripStorage;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.models.trip_k.PendingLocation;
import com.pitstop.network.RequestError;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

/**
 * Created by Karol Zdebel on 3/28/2018.
 */

@RunWith(AndroidJUnit4.class)
@LargeTest
public class EndTripUseCaseTest {

    private final String TAG = EndTripUseCaseTest.class.getSimpleName();

    private UseCaseComponent useCaseComponent;
    private LocalPendingTripStorage localPendingTripStorage;

    @Before
    public void setup(){
        Log.i(TAG,"running setup()");
        Context context = InstrumentationRegistry.getTargetContext();
        useCaseComponent = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(context))
                .build();
        localPendingTripStorage = new LocalPendingTripStorage(context);
        localPendingTripStorage.deleteAll();
    }

    @Test
    public void endTripUseCaseTest(){
        Log.d(TAG,"endTripUseCaseTest()");

        CompletableFuture<List<PendingLocation>> result = new CompletableFuture<>();

        List<Location> trip = new ArrayList<>();
        List<PendingLocation> tripPending = new ArrayList<>();
        for (int i=0;i<3;i++){
            Location randomLocation = Util.Companion.getRandomLocation();
            tripPending.add(new PendingLocation(randomLocation.getLongitude()
                    ,randomLocation.getLatitude(),randomLocation.getTime()/1000));
            trip.add(randomLocation);
        }

        useCaseComponent.endTripUseCase().execute(trip, new EndTripUseCase.Callback() {
            @Override
            public void finished(@NotNull List<PendingLocation> loc) {
                Log.d(TAG,"finished() trip: "+loc);
                result.complete(loc);
            }

            @Override
            public void onError(@NotNull RequestError err) {
                Log.d(TAG,"onError() err: "+err);
                result.complete(null);
            }
        });

        try{
            List<PendingLocation> tripResult = result.get(10000, TimeUnit.MILLISECONDS);
            assertNotNull(trip);
            assertEquals(new HashSet(tripPending),new HashSet(tripResult));
        }catch(InterruptedException | ExecutionException | TimeoutException e){
            e.printStackTrace();
        }
    }

}
