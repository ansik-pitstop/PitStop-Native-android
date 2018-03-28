package com.pitstop.interactors.add;

import android.content.Context;
import android.location.Location;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.pitstop.Util;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.models.Car;
import com.pitstop.network.RequestError;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static junit.framework.Assert.assertTrue;

/**
 * Created by Karol Zdebel on 3/16/2018.
 */

@RunWith(AndroidJUnit4.class)
@LargeTest
public class AddTripUseCaseTest {

    private final String TAG = AddTripUseCaseTest.class.getSimpleName();

    private UseCaseComponent useCaseComponent;

    @Before
    public void setup(){
        Log.i(TAG,"running setup()");
        Context context = InstrumentationRegistry.getTargetContext();
        useCaseComponent = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(context))
                .build();

    }

    @Test
    public void addTripUseCaseTest(){
        Log.i(TAG,"starting addTripUseCaseTest");

        Car dummyCar = new Car();
        dummyCar.setVin("1GB0CVCL7BF147611");
        dummyCar.setId(6014);

        List<Location> trip = new ArrayList<>();
        for (int i=0;i<3;i++){
            trip.add(Util.Companion.getRandomLocation());
        }

        CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
        useCaseComponent.getAddTripUseCase().execute(trip, new AddTripDataUseCase.Callback() {
            @Override
            public void onAddedTripData() {
                completableFuture.complete(true);
                Log.i(TAG,"addTripUseCaseTest: onAddedTripData()");
            }

            @Override
            public void onError(@NotNull RequestError err) {
                completableFuture.complete(false);
                Log.i(TAG,"addTripUseCaseTest: onError()");
            }
        });

        try{
            assertTrue(completableFuture.get(10000, TimeUnit.MILLISECONDS).equals(true));
        }catch(InterruptedException | ExecutionException | TimeoutException e){
            e.printStackTrace();
        }
    }
}
