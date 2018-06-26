package com.pitstop.interactors.other;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.pitstop.database.LocalCarStorage;
import com.pitstop.database.LocalDatabaseHelper;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.models.PendingUpdate;
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

import static junit.framework.Assert.assertEquals;

/**
 * Created by Karol Zdebel on 5/31/2018.
 */

@RunWith(AndroidJUnit4.class)
@LargeTest
public class SendPendingUpdatesUseCaseTest {

    private final String TAG = SendPendingUpdatesUseCaseTest.class.getSimpleName();

    private UseCaseComponent useCaseComponent;
    private LocalCarStorage localCarStorage;

    @Before
    public void setup(){
        Context context = InstrumentationRegistry.getTargetContext();
        useCaseComponent = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(context)).build();
        localCarStorage = new LocalCarStorage(LocalDatabaseHelper.getInstance(context));
    }

    @Test
    public void sendPendingUpdatesTest(){
        Log.d(TAG,"sendPendingUpdatesTest()");

        CompletableFuture<PendingUpdate> result = new CompletableFuture<>();
        int carId = 6164;
        double mileage = 512340.5;

        localCarStorage.removeAllPendingUpdates();

        PendingUpdate pendingUpdate = new PendingUpdate(carId,PendingUpdate.CAR_MILEAGE_UPDATE
                ,""+mileage,System.currentTimeMillis());
        localCarStorage.storePendingUpdate(pendingUpdate);
        useCaseComponent.sendPendingUpdatesUseCase().execute(new SendPendingUpdatesUseCase.Callback() {
            @Override
            public void updatesSent(@NotNull List<PendingUpdate> pendingUpdates) {
                Log.d(TAG,"updatesSent() pendingUpdates: "+pendingUpdates);
                result.complete(pendingUpdates.get(0));
            }

            @Override
            public void errorSending(@NotNull RequestError err) {
                Log.d(TAG,"err: "+err);
                result.complete(null);
            }
        });

        try{
            PendingUpdate pendingUpdateResult = result.get(10000, TimeUnit.MILLISECONDS);
            assertEquals(pendingUpdate,pendingUpdateResult);
            localCarStorage.removeAllPendingUpdates();
        }catch(InterruptedException | ExecutionException | TimeoutException e){
            e.printStackTrace();
        }

    }
}
