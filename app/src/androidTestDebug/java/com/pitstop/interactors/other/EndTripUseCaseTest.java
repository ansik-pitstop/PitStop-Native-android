package com.pitstop.interactors.other;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.pitstop.TripTestUtil;
import com.pitstop.database.LocalPendingTripStorage;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.models.trip.RecordedLocation;
import com.pitstop.network.RequestError;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static junit.framework.Assert.assertTrue;

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

        CompletableFuture<Boolean> result = new CompletableFuture<>();

        List<RecordedLocation> trip = TripTestUtil.Companion.getRandomRoute(120, EndTripUseCase.MIN_CONF+1);

        useCaseComponent.endTripUseCase().execute(trip, new EndTripUseCase.Callback() {
            @Override
            public void tripDiscarded() {
                Log.d(TAG,"tripDiscarded()");
                result.complete(false);
            }

            @Override
            public void finished() {
                Log.d(TAG,"finished()");
                result.complete(true);
            }

            @Override
            public void onError(@NotNull RequestError err) {
                Log.d(TAG,"onError() err: "+err);
                result.complete(false);
            }
        });

        try{
            Boolean tripResult = result.get(10000, TimeUnit.MILLISECONDS);
            assertTrue(tripResult);
        }catch(InterruptedException | ExecutionException | TimeoutException e){
            e.printStackTrace();
        }
        localPendingTripStorage.deleteAll();
    }

    @Test
    public void endLowConfidenceTripTest(){
        Log.d(TAG,"endLowConfidenceTripTest()");

        CompletableFuture<Boolean> result = new CompletableFuture<>();

        List<RecordedLocation> trip = TripTestUtil.Companion
                .getRandomRoute(120,EndTripUseCase.MIN_CONF-1);

        useCaseComponent.endTripUseCase().execute(trip, new EndTripUseCase.Callback() {
            @Override
            public void tripDiscarded() {
                Log.d(TAG,"tripDiscarded()");
                result.complete(true);
            }

            @Override
            public void finished() {
                Log.d(TAG,"finished()");
                result.complete(false);
            }

            @Override
            public void onError(@NotNull RequestError err) {
                Log.d(TAG,"onError() err: "+err);
                result.complete(false);
            }
        });

        try{
            Boolean tripResult = result.get(10000, TimeUnit.MILLISECONDS);
            assertTrue(tripResult);
        }catch(InterruptedException | ExecutionException | TimeoutException e){
            e.printStackTrace();
        }
        localPendingTripStorage.deleteAll();
    }

}
