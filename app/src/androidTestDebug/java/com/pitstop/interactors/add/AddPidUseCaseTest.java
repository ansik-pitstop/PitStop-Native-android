package com.pitstop.interactors.add;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.pitstop.SensorDataTestUtil;
import com.pitstop.bluetooth.dataPackages.OBD215PidPackage;
import com.pitstop.database.LocalSensorDataStorage;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
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
 * Created by Karol Zdebel on 4/24/2018.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class AddPidUseCaseTest {

    private final String TAG = AddPidUseCaseTest.class.getSimpleName();

    private UseCaseComponent useCaseComponent;
    private LocalSensorDataStorage localSensorDataStorage;

    private final String VIN = "1GB0CVCL7BF147611";
    private final String deviceId = "215B002373";

    @Before
    public void setup(){
        Log.i(TAG,"running setup()");
        Context context = InstrumentationRegistry.getTargetContext();
        localSensorDataStorage = new LocalSensorDataStorage(context);
        useCaseComponent = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(context))
                .build();

    }

    @Test
    public void addPidUseCaseTest(){
        Log.i(TAG,"starting addPidUseCaseTest");

        localSensorDataStorage.deleteAll();

        CompletableFuture<Boolean> future = new CompletableFuture<>();

        List<OBD215PidPackage> dataToBeSent = SensorDataTestUtil.get215PidData(120, deviceId,0);
        Log.d(TAG,"data to be sent size: "+dataToBeSent.size());
        for (int i=0;i<dataToBeSent.size();i++){
            final int index = i;
            useCaseComponent.addPidUseCase().execute(dataToBeSent.get(i), VIN, new AddPidUseCase.Callback() {
                @Override
                public void onAdded(int size) {
                    Log.d(TAG,"onAdded() size: "+size+", index = "+index);
                    if (((index+1) % 20) != 0)
                        future.complete(false);

                    if (index == dataToBeSent.size()-1) future.complete(true);
                }

                @Override
                public void onStoredLocally(int size) {
                    Log.d(TAG,"onStoredLocally() size: "+size+", index = "+index+", index+1 %20 = "+((index+1)%20));
                    //Chunk size is 20
                    if ((index+1) % 20 == 0){
                        future.complete(false);
                    }
                }

                @Override
                public void onError(@NotNull RequestError error) {
                    Log.d(TAG,"onError() error: "+error);
                    future.complete(false);
                }
            });
        }

        try{
            assertTrue(future.get(20000, TimeUnit.MILLISECONDS).equals(true));
            localSensorDataStorage.deleteAll();
        }catch(InterruptedException | ExecutionException | TimeoutException e){
            e.printStackTrace();
        }
    }

}
