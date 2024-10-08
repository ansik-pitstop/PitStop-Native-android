package com.pitstop.repositories;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.util.Log;

import com.pitstop.SensorDataTestUtil;
import com.pitstop.database.LocalDatabaseHelper;
import com.pitstop.database.LocalSensorDataStorage;
import com.pitstop.models.sensor_data.SensorData;
import com.pitstop.retrofit.PitstopSensorDataApi;

import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

import static junit.framework.Assert.assertTrue;

/**
 * Created by Karol Zdebel on 4/20/2018.
 */

public class SensorDataRepositoryTest {
    private final String TAG = SensorDataRepositoryTest.class.getSimpleName();

    private SensorDataRepository sensorDataRepository;
    private LocalSensorDataStorage localSensorDataStorage;
    private final String VIN = "1GB0CVCL7BF147611";
    private final String deviceID = "215B002373";
    private final String VIN2 = "1GB0CVCL7BF147613";
    private final String deviceID2 = "215B002286";

    @Before
    public void setup() {
        Log.i(TAG, "running setup()");
        Context context = InstrumentationRegistry.getTargetContext();
        PitstopSensorDataApi pitstopSensorDataApi = RetrofitTestUtil.Companion.getSensorDataApi();
        localSensorDataStorage = new LocalSensorDataStorage(LocalDatabaseHelper.getInstance(context));
        sensorDataRepository = new SensorDataRepository(localSensorDataStorage
                ,pitstopSensorDataApi, Observable.just(false));
        localSensorDataStorage.deleteAll();
    }

    @Test
    public void storeSensorDataTest(){
        Log.d(TAG,"running storeTripTest()");
        CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();

        Collection<SensorData> sensorData = SensorDataTestUtil.get215SensorData(2,deviceID,VIN,0);
        sensorData.addAll(SensorDataTestUtil.get215SensorData(2,deviceID2,VIN2,2));
        Log.d(TAG,"generated sensor data "+sensorData);
        sensorDataRepository.storeThenDump(sensorData)
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.io(),true)
                .subscribe(next -> {
                    Log.d(TAG,"next() : "+next);
                    if (next > 0){
                        completableFuture.complete(true);
                    }else{
                        completableFuture.complete(false);
                    }

                }, error -> {
                    completableFuture.complete(false);
                    Log.d(TAG,"error() : "+error);
                });

        try{
            assertTrue(completableFuture.get(11000, TimeUnit.MILLISECONDS));
            assertTrue(localSensorDataStorage.getAll().isEmpty());
        }catch(ExecutionException | InterruptedException | TimeoutException e){
            throw new AssertionError();
        }

    }

}
