package com.pitstop.interactors.other;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.network.RequestError;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static junit.framework.Assert.assertTrue;

/**
 * Created by Karol Zdebel on 5/14/2018.
 */

@RunWith(AndroidJUnit4.class)
@LargeTest
public class DeviceClockSyncUseCaseTest {

    //This test is dependent on the car currently selected in the app at the time it is run!

    private String TAG = getClass().getSimpleName();
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
    public void deviceClockSyncTest(){
        String deviceId = "215B002373";
        long rtcTime = 10000;
        String vin = "";

        CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();

        useCaseComponent.getDeviceClockSyncUseCase().execute(rtcTime, deviceId, vin, new DeviceClockSyncUseCase.Callback() {
            @Override
            public void onClockSynced() {
                Log.d(TAG,"onClockSynced()");
                completableFuture.complete(true);
            }

            @Override
            public void onError(@NotNull RequestError error) {
                Log.d(TAG,"onError() err: "+error);
                completableFuture.complete(false);
            }
        });

        try{
            Boolean tripResult = completableFuture.get(10000, TimeUnit.MILLISECONDS);
            assertTrue(tripResult);
        }catch(InterruptedException | ExecutionException | TimeoutException e){
            e.printStackTrace();
        }
    }
}
