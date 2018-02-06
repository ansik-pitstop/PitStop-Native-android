package com.pitstop.interactors.get;

import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;

import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.network.RequestError;
import com.pitstop.retrofit.PredictedService;

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

    private UseCaseComponent useCaseComponent;

    @Before
    public void setup(){
        useCaseComponent = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(InstrumentationRegistry.getContext()))
                .build();
    }

    @Test
    public void testPredictedServiceUseCase(){

        CompletableFuture<PredictedService> future = new CompletableFuture<>();
        System.out.println("running testPredictedServiceUseCase()");
        useCaseComponent.getPredictedServiceDateUseCase().execute(new GetPredictedServiceUseCase.Callback() {
            @Override
            public void onGotPredictedService(@NotNull PredictedService predictedService) {
                System.out.println("onGotPredictedService() predictedService: "+predictedService);
                future.complete(predictedService);
            }

            @Override
            public void onError(@NotNull RequestError error) {
                System.out.println("onError() error: "+error);
                future.complete(null);
            }
        });

        try{
            Assert.assertNotNull(future.get(2000, TimeUnit.MILLISECONDS));
        }catch(InterruptedException | ExecutionException | TimeoutException e){
            e.printStackTrace();
        }
    }
}
