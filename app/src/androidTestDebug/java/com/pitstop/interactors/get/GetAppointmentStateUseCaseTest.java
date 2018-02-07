package com.pitstop.interactors.get;

import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.models.Appointment;
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
 * Created by Karol Zdebel on 2/7/2018.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class GetAppointmentStateUseCaseTest {

    private final String TAG = GetAppointmentStateUseCaseTest.class.getSimpleName();
    private UseCaseComponent useCaseComponent;
    private boolean connected = true;

    @Before
    public void setup(){
        useCaseComponent = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(InstrumentationRegistry.getTargetContext()))
                .build();
        connected = NetworkHelper.isConnected(InstrumentationRegistry.getTargetContext());
    }

    @Test
    public void testGetAppointmentStateUseCase(){
        Log.d(TAG,"running testGetAppointmentStateUseCase() connected? "+connected);

        CompletableFuture<Object> completableFuture = new CompletableFuture<>();

        useCaseComponent.getAppointmentStateUseCase().execute(new GetAppointmentStateUseCase.Callback() {
            @Override
            public void onPredictedServiceState(@NotNull PredictedService predictedService) {
                Log.d(TAG,"onPredictedServiceState() predictedServie: "+predictedService);
                completableFuture.complete(predictedService);
            }

            @Override
            public void onAppointmentBookedState(@NotNull Appointment appointment) {
                Log.d(TAG,"onAppointmentBookedState() appointment: "+appointment);
                completableFuture.complete(appointment);
            }

            @Override
            public void onMileageUpdateNeededState() {
                Log.d(TAG,"onMileageUpdateNeededState()");
                completableFuture.complete(new Object());
            }

            @Override
            public void onError(@NotNull RequestError error) {
                Log.d(TAG,"onError() error: "+error);
                completableFuture.complete(error);
            }
        });

        try{
            Object futureObject = completableFuture.get(5000, TimeUnit.MILLISECONDS);
            if (connected){
                Assert.assertNotNull(futureObject);
            }
            else{
                Assert.assertTrue(futureObject instanceof RequestError && ((RequestError)futureObject)
                        .getError().equals(RequestError.ERR_OFFLINE));
            }

        }catch(InterruptedException | ExecutionException | TimeoutException e){
            e.printStackTrace();
        }
    }
}
