package com.pitstop.interactors.get;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.pitstop.TripTestUtil;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.models.snapToRoad.SnappedPoint;
import com.pitstop.models.trip.RecordedLocation;
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
 * Created by David C. on 22/3/18.
 */

@RunWith(AndroidJUnit4.class)
@LargeTest
public class GetSnapToRoadUseCaseTest {

    private final String TAG = GetSnapToRoadUseCaseTest.class.getSimpleName();
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
    public void getSnapToRoadUseCaseTest() {

        List<RecordedLocation> path = TripTestUtil.Companion.getRandomRoute(220, 91);

        CompletableFuture<List<SnappedPoint>> successFuture = new CompletableFuture<>();
        CompletableFuture<RequestError> errorFuture = new CompletableFuture<>();

        useCaseComponent.getSnapToRoadUseCase().execute(path, new GetSnapToRoadUseCase.Callback() {

            @Override
            public void onSnapToRoadRetrieved(@NotNull List<? extends SnappedPoint> snappedPointList) {
                Log.d(TAG, "GetSnapToRoadUseCaseTest onSnapToRoadRetrieved() size: " + snappedPointList.size());
                successFuture.complete((List<SnappedPoint>) snappedPointList);
                errorFuture.complete(null);
            }

            @Override
            public void onError(@NotNull RequestError error) {
                successFuture.complete(null);
                errorFuture.complete(error);
                Log.i(TAG, "GetSnapToRoadUseCaseTest onError()");
            }

        });

        try {
            if (connected) {
                List<SnappedPoint> result = successFuture.get(5000, TimeUnit.MILLISECONDS);
                Assert.assertNotNull(result);
                Assert.assertTrue(path.size() <= result.size());
            } else
                Assert.assertTrue(errorFuture.get(5000, TimeUnit.MILLISECONDS)
                        .getError().equals(RequestError.ERR_OFFLINE));
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }

    }
}
