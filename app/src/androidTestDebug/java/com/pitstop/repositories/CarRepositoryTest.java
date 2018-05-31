package com.pitstop.repositories;

import android.content.Context;
import android.preference.PreferenceManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.pitstop.database.LocalCarStorage;
import com.pitstop.models.PendingUpdate;
import com.pitstop.retrofit.PitstopAuthApi;
import com.pitstop.utils.NetworkHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static junit.framework.Assert.assertTrue;

/**
 * Created by Karol Zdebel on 5/29/2018.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class CarRepositoryTest {

    private final String TAG = CarRepositoryTest.class.getSimpleName();
    private CarRepository carRepository;
    private LocalCarStorage localCarStorage;

    @Before
    public void setup(){
        Context context = InstrumentationRegistry.getTargetContext();
        localCarStorage = new LocalCarStorage(context);
        PitstopAuthApi pitstopAuthApi = RetrofitTestUtil.Companion.getAuthApi();
        NetworkHelper networkHelper = new NetworkHelper(context,pitstopAuthApi, PreferenceManager.getDefaultSharedPreferences(context));
        carRepository = new CarRepository(localCarStorage,networkHelper
                ,RetrofitTestUtil.Companion.getCarApi());
    }

    //This test makes sure that if an update fails, that an update is then stored in the database
    @Test
    //Have phone offline for this test
    public void checkPendingUpdateStoredTest(){
        Log.d(TAG,"running checkPendingUpdateStoredTest()");
        int carId = 6164;
        double mileage = 400000;

        localCarStorage.removeAllPendingUpdates();
        CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();

        carRepository.updateMileage(carId, mileage)
                .subscribe(next -> {
                    completableFuture.complete(false);
                }, err -> {
                    Log.d(TAG,"pending updates after mileage update: "
                            +localCarStorage.getPendingUpdates());
                    if (localCarStorage.getPendingUpdates().get(0).getValue().equals(""+mileage)){
                        completableFuture.complete(true);
                    }else completableFuture.complete(false);
                });

        try{
            assertTrue(completableFuture
                    .get(2000, java.util.concurrent.TimeUnit.MILLISECONDS));
        }catch( InterruptedException | ExecutionException | TimeoutException e){
            e.printStackTrace();
        }

    }

    //This test makes sure that if the pending update is in the database, that it is then sent
    @Test
    public void sendPendingUpdatesTest(){
        Log.d(TAG,"running sendPendingUpdatesTest()");
        int carId = 6164;
        double mileageBefore = 400000;
        double mileage = 500000;

        CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();

        localCarStorage.storePendingUpdate(new PendingUpdate(carId, PendingUpdate.CAR_MILEAGE_UPDATE
                ,""+mileage,System.currentTimeMillis()));
        carRepository.updateMileage(carId,400000)
                .subscribe(mileageBeforeUpdate -> {
                   Log.d(TAG,"updated mileage to "+mileageBefore);
                    carRepository.sendPendingUpdates()
                            .subscribe(next -> {
                                Log.d(TAG,"sendPendingUpdates() response: "+next);
                                carRepository.get(carId)
                                        .subscribe(car -> {
                                            if (car.isLocal()) return;
                                            if (car.getData() == null){
                                                Log.d(TAG,"car data is null");
                                                completableFuture.complete(false);
                                            }else{
                                                Log.d(TAG,"got car, mileage: "+car.getData().getTotalMileage());
                                                if (car.getData().getTotalMileage() == mileage){
                                                    completableFuture.complete(true);
                                                }else{
                                                    completableFuture.complete(false);
                                                }
                                            }
                                        }, err -> {
                                            completableFuture.complete(false);
                                        });
                            },err -> {
                                completableFuture.complete(false);
                                Log.d(TAG,"sendPendingUpdates() error: "+err);
                            });

                }, err -> {
                    Log.d(TAG,"error updating mileage to "+mileageBefore+", err: "+err);
                    completableFuture.complete(false);
                });

        try{
            assertTrue(completableFuture
                    .get(2000, java.util.concurrent.TimeUnit.MILLISECONDS));
        }catch( InterruptedException | ExecutionException | TimeoutException e){
            e.printStackTrace();
        }


    }

}
