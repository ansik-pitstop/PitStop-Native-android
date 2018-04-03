package com.pitstop.repositories;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pitstop.application.Constants;
import com.pitstop.database.LocalTripStorage;
import com.pitstop.models.Car;
import com.pitstop.models.trip.Trip;
import com.pitstop.retrofit.PitstopTripApi;

import org.json.JSONArray;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Type;
import java.util.ArrayList;
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
    private LocalTripStorage localTripStorage;

    @Before
    public void setup() {
        Log.i(TAG, "running setup()");
        Context context = InstrumentationRegistry.getTargetContext();
        PitstopTripApi api = RetrofitTestUtil.Companion.getTripApi();
        LocalTripStorage localTripStorage = new LocalTripStorage(context);

        tripRepository = new TripRepository(localTripStorage, api);

        localTripStorage = new LocalTripStorage(context);
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

                    Log.i(TAG, "Got trips: " + next);
                    future.complete(next.getData());

                }).subscribe();

        try {
            assertTrue(future
                    .get(2000, java.util.concurrent.TimeUnit.MILLISECONDS).size() > 0);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void deleteAllTripsFromCarVinTest() {

        Trip trip = generateTrip();

        List<Trip> tripList = new ArrayList<>();
        tripList.add(trip);

        localTripStorage.deleteAndStoreTripList(tripList);

        CompletableFuture<String> future = new CompletableFuture<>();

        tripRepository.deleteAllTripsFromCarVin(trip.getVin(), new Repository.Callback<Object>() {
        });

    }

    @Test
    public void deleteTrip() {

        Trip trip = generateTrip();

        List<Trip> tripList = new ArrayList<>();
        tripList.add(trip);

        localTripStorage.deleteAndStoreTripList(tripList);

        CompletableFuture<String> future = new CompletableFuture<>();

        tripRepository.deleteTrip(trip.getTripId(), trip.getVin()).doOnNext(stringPitstopResponse -> {

            future.complete(stringPitstopResponse.getResponse());

        }).subscribe();

        try {
            assertTrue(future
                    .get(2000, java.util.concurrent.TimeUnit.MILLISECONDS).equals("Success"));
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }

    }

    private Trip generateTrip() {

        String response = "[\n" +
                "        {\n" +
                "            \"_id\": 14759,\n" +
                "            \"tripId\": \"1520629854\",\n" +
                "            \"locationStart\": {\n" +
                "                \"altitude\": null,\n" +
                "                \"latitude\": \"43.6186889945396\",\n" +
                "                \"longitude\": \"-79.536713666964\",\n" +
                "                \"startLocation\": null,\n" +
                "                \"startCityLocation\": null,\n" +
                "                \"startStreetLocation\": null\n" +
                "            },\n" +
                "            \"locationEnd\": {\n" +
                "                \"altitude\": null,\n" +
                "                \"latitude\": \"43.6186889945396\",\n" +
                "                \"longitude\": \"-79.536713666964\",\n" +
                "                \"endLocation\": null,\n" +
                "                \"endCityLocation\": null,\n" +
                "                \"endStreetLocation\": null\n" +
                "            },\n" +
                "            \"mileageStart\": null,\n" +
                "            \"fuelConsumptionAccum\": null,\n" +
                "            \"fuelConsumptionStart\": null,\n" +
                "            \"mileageAccum\": \"0.0\",\n" +
                "            \"timeStart\": 1520629854,\n" +
                "            \"timeEnd\": 1520631928,\n" +
                "            \"vin\": \"WVWXK73C37E116278\",\n" +
                "            \"alarms\": [],\n" +
                "            \"location_polyline\": [\n" +
                "                {\n" +
                "                    \"location\": [\n" +
                "                        {\n" +
                "                            \"id\": \"longitude\",\n" +
                "                            \"data\": \"-79.536713666964\"\n" +
                "                        },\n" +
                "                        {\n" +
                "                            \"id\": \"latitude\",\n" +
                "                            \"data\": \"43.6186889945396\"\n" +
                "                        }\n" +
                "                    ],\n" +
                "                    \"timestamp\": \"1520629854\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"location\": [\n" +
                "                        {\n" +
                "                            \"id\": \"longitude\",\n" +
                "                            \"data\": \"-79.536713666964\"\n" +
                "                        },\n" +
                "                        {\n" +
                "                            \"id\": \"latitude\",\n" +
                "                            \"data\": \"43.6186889945396\"\n" +
                "                        }\n" +
                "                    ],\n" +
                "                    \"timestamp\": \"1520629854\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"location\": [\n" +
                "                        {\n" +
                "                            \"id\": \"longitude\",\n" +
                "                            \"data\": \"-79.536713666964\"\n" +
                "                        },\n" +
                "                        {\n" +
                "                            \"id\": \"latitude\",\n" +
                "                            \"data\": \"43.6186889945396\"\n" +
                "                        }\n" +
                "                    ],\n" +
                "                    \"timestamp\": \"1520629854\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"location\": [\n" +
                "                        {\n" +
                "                            \"id\": \"longitude\",\n" +
                "                            \"data\": \"-79.536713666964\"\n" +
                "                        },\n" +
                "                        {\n" +
                "                            \"id\": \"latitude\",\n" +
                "                            \"data\": \"43.6186889945396\"\n" +
                "                        }\n" +
                "                    ],\n" +
                "                    \"timestamp\": \"1520631928\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"location\": [\n" +
                "                        {\n" +
                "                            \"id\": \"longitude\",\n" +
                "                            \"data\": \"-79.536713666964\"\n" +
                "                        },\n" +
                "                        {\n" +
                "                            \"id\": \"latitude\",\n" +
                "                            \"data\": \"43.6186889945396\"\n" +
                "                        }\n" +
                "                    ],\n" +
                "                    \"timestamp\": \"1520631928\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"location\": [\n" +
                "                        {\n" +
                "                            \"id\": \"longitude\",\n" +
                "                            \"data\": \"-79.536713666964\"\n" +
                "                        },\n" +
                "                        {\n" +
                "                            \"id\": \"latitude\",\n" +
                "                            \"data\": \"43.6186889945396\"\n" +
                "                        }\n" +
                "                    ],\n" +
                "                    \"timestamp\": \"1520631928\"\n" +
                "                }\n" +
                "            ]\n" +
                "        }\n" +
                "    ]";

        Trip trip = new Trip();

        try {

            JSONArray jsonArray = new JSONArray(response);

            Gson gson = new Gson();
            //TypeToken<List<Trip>> listType = new TypeToken<List<Trip>>();
            Type typeOfT = new TypeToken<List<Trip>>() {
            }.getType();
            List<Trip> tripList = gson.fromJson(String.valueOf(jsonArray), typeOfT);

            trip = tripList.get(0);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return trip;

    }

}
