package com.pitstop.repositories;

import android.content.Context;
import android.location.Geocoder;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.pitstop.Util;
import com.pitstop.database.LocalPendingTripStorage;
import com.pitstop.database.LocalTripStorage;
import com.pitstop.models.trip.TripData;
import com.pitstop.retrofit.PitstopTripApi;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

import static junit.framework.Assert.assertTrue;

/**
 * Created by Karol Zdebel on 3/21/2018.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class TripRepositoryTest {

    private final String TAG = TripRepositoryTest.class.getSimpleName();

    private TripRepository tripRepository;

    private final String VIN = "1GB0CVCL7BF147611";

    @Before
    public void setup(){
        Log.i(TAG,"running setup()");
        Context context = InstrumentationRegistry.getTargetContext();
        PitstopTripApi api = RetrofitTestUtil.Companion.getTripApi();
        LocalPendingTripStorage localPendingTripStorage = new LocalPendingTripStorage(context);
        LocalTripStorage localTripStorage = new LocalTripStorage(context);
        tripRepository = new TripRepository(api,localPendingTripStorage,localTripStorage
                , new Geocoder(context),Observable.just(true));
    }

    @Test
    public void storeTripTest(){
        Log.d(TAG,"running storeTripTest()");
        CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();

        TripData tripData = Util.Companion.generateTripData(3,VIN,System.currentTimeMillis());
        Log.d(TAG,"generated trip data, location size = "+tripData.getLocations().size());
        tripRepository.storeTripData(tripData)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io(),true)
                .subscribe(next -> {
                    completableFuture.complete(true);
                    Log.d(TAG,"next() : "+next);
                }, error -> {
                    completableFuture.complete(false);
                    Log.d(TAG,"error() : "+error);
                });

        try{
            assertTrue(completableFuture.get(11000, TimeUnit.MILLISECONDS));
        }catch(ExecutionException | InterruptedException | TimeoutException e){
            throw new AssertionError();
        }

    }

//    @Before
//    public void setup() {
//        Log.i(TAG, "running setup()");
//        Context context = InstrumentationRegistry.getTargetContext();
//        PitstopTripApi api = RetrofitTestUtil.Companion.getTripApi();
//
//        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(context, "trips-db");
//        Database db = helper.getWritableDb();
//        DaoSession daoSession = new DaoMaster(db).newSession();
//
//        tripRepository = new TripRepository(daoSession, api);
//    }
//
//    @Test
//    public void getTripsByCarVinTest() {
//
//        Log.i(TAG, "running getTripsByCarVinTest()");
//
//        //Input
//        CompletableFuture<List<Trip>> future = new CompletableFuture<>();
//        Car dummyCar = new Car();
//        dummyCar.setVin("WVWXK73C37E116278");
//        dummyCar.setId(5942);
//
//        String whatToReturn = Constants.TRIP_REQUEST_BOTH;
//
//        tripRepository.getTripsByCarVin(dummyCar.getVin(), whatToReturn)
//                .doOnNext(next -> {
//
//                    Log.i(TAG, "Got appointments: " + next);
//                    future.complete(next.getData());
//
//                }).subscribe();
//
//        try {
//            assertTrue(future
//                    .get(2000, java.util.concurrent.TimeUnit.MILLISECONDS).size() > 0);
//        } catch (InterruptedException | ExecutionException | TimeoutException e) {
//            e.printStackTrace();
//        }
//
//    }
}
