package com.pitstop.interactors.get;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.models.Car;
import com.pitstop.models.trip.Trip;
import com.pitstop.network.RequestError;
import com.pitstop.utils.NetworkHelper;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by David C. on 19/3/18.
 */

@RunWith(AndroidJUnit4.class)
@LargeTest
public class GetTripsUseCaseTest {

    private final String TAG = GetTripsUseCaseTest.class.getSimpleName();
    private UseCaseComponent useCaseComponent;
    private boolean connected = true;

    @Before
    public void setup() {
        Log.i(TAG, "running setup()");
        Context context = InstrumentationRegistry.getTargetContext();
        useCaseComponent = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(context))
                .build();
        connected = NetworkHelper.isConnected(InstrumentationRegistry.getTargetContext());

    }

    @Test
    public void getTripsUseCaseTest() {

        Car dummyCar = new Car();
        dummyCar.setVin("WVWXK73C37E116278");
        dummyCar.setId(5942);


        CompletableFuture<List<Trip>> successFuture = new CompletableFuture<>();
        CompletableFuture<RequestError> errorFuture = new CompletableFuture<>();

        useCaseComponent.getTripsUseCase().execute(new GetTripsUseCase.Callback() {
            @Override
            public void onNoCar() {
                errorFuture.complete(RequestError.getUnknownError());
                successFuture.complete(null);

            }

            @Override
            public void onTripsRetrieved(@NotNull List<? extends Trip> tripList, boolean isLocal) {
                Log.d(TAG, "GetTripsUseCaseTest onTripsRetrieved(): " + tripList);
                successFuture.complete((List<Trip>) tripList);
                errorFuture.complete(null);
            }

            @Override
            public void onError(@NotNull RequestError error) {
                successFuture.complete(null);
                errorFuture.complete(error);
                Log.i(TAG, "GetTripsUseCaseTest onError()");
            }
        });

        try {
            if (connected)
                Assert.assertNotNull(successFuture.get(5000, TimeUnit.MILLISECONDS));
            else
                Assert.assertTrue(errorFuture.get(5000, TimeUnit.MILLISECONDS)
                        .getError().equals(RequestError.ERR_OFFLINE));
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }

    }

}
