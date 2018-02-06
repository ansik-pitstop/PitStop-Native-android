package com.pitstop.interactors.get;

import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.network.RequestError;
import com.pitstop.retrofit.PredictedService;
import com.pitstop.utils.NetworkHelper;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by Karol Zdebel on 2/6/2018.
 */

@RunWith(AndroidJUnit4.class)
@LargeTest
public class GetPredictedServiceUseCaseTest {

    private final String TAG = GetPredictedServiceUseCaseTest.class.getSimpleName();
    private UseCaseComponent useCaseComponent;
    private boolean connected = true;

    @Before
    public void setup(){
        useCaseComponent = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(InstrumentationRegistry.getTargetContext()))
                .build();
        connected = NetworkHelper.isConnected(InstrumentationRegistry.getTargetContext());

    }

    //So far only works for cars which have a predicted service date available
    @Test
    public void testPredictedServiceUseCase(){
        CompletableFuture<PredictedService> successFuture = new CompletableFuture<>();
        CompletableFuture<RequestError> errorFuture = new CompletableFuture<>();
        Log.i(TAG,"running testPredictedServiceUseCase() connected? "+connected);
        useCaseComponent.getPredictedServiceDateUseCase().execute(new GetPredictedServiceUseCase.Callback() {
            @Override
            public void onGotPredictedService(@NotNull PredictedService predictedService) {
                Log.i(TAG,"onGotPredictedService() predictedService: "+predictedService);
                successFuture.complete(predictedService);
                errorFuture.complete(null);
            }

            @Override
            public void onError(@NotNull RequestError error) {
                Log.i(TAG,"onError() error: "+error);
                successFuture.complete(null);
                errorFuture.complete(error);
            }
        });

        try{
            if (connected)
                Assert.assertNotNull(successFuture.get(5000, TimeUnit.MILLISECONDS));
            else
                Assert.assertTrue(errorFuture.get(5000, TimeUnit.MILLISECONDS)
                        .getError().equals(RequestError.ERR_OFFLINE));
        }catch(InterruptedException | ExecutionException | TimeoutException e){
            e.printStackTrace();
        }
    }
}
