package com.pitstop.repositories;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.pitstop.application.Constants;
import com.pitstop.models.Car;
import com.pitstop.models.trip.DaoMaster;
import com.pitstop.models.trip.DaoSession;
import com.pitstop.models.trip.Trip;
import com.pitstop.retrofit.PitstopTripApi;

import org.greenrobot.greendao.database.Database;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static junit.framework.Assert.assertTrue;

/**
 * Created by David C. on 22/3/18.
 */

@RunWith(AndroidJUnit4.class)
@LargeTest
public class TripRepositoryTest {

    private final String TAG = AppointmentRepositoryTest.class.getSimpleName();
    private TripRepository tripRepository;

    @Before
    public void setup() {
        Log.i(TAG, "running setup()");
        Context context = InstrumentationRegistry.getTargetContext();
        PitstopTripApi api = RetrofitTestUtil.Companion.getTripApi();

        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(context, "trips-db");
        Database db = helper.getWritableDb();
        DaoSession daoSession = new DaoMaster(db).newSession();

        tripRepository = new TripRepository(daoSession, api);
    }

    @Test
    public void getTripsByCarVinTest() {

        Log.i(TAG, "running getTripsByCarVinTest()");

        //Input
        CompletableFuture<List<Trip>> future = new CompletableFuture<>();
        Car dummyCar = new Car();
        dummyCar.setVin("WVWXK73C37E116278");
        dummyCar.setId(5942);

        String whatToReturn = Constants.TRIP_REQUEST_BOTH;

        tripRepository.getTripsByCarVin(dummyCar.getVin(), whatToReturn)
                .doOnNext(next -> {

                    Log.i(TAG, "Got appointments: " + next);
                    future.complete(next.getData());

                }).subscribe();

        try {
            assertTrue(future
                    .get(2000, java.util.concurrent.TimeUnit.MILLISECONDS).size() > 0);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }

    }

}
