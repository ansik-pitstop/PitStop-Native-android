package com.pitstop.interactors.remove;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.models.trip.Trip;
import com.pitstop.network.RequestError;
import com.pitstop.utils.NetworkHelper;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by David C. on 2/4/18.
 */

@RunWith(AndroidJUnit4.class)
@LargeTest
public class RemoveTripUseCaseTest {

    private final String TAG = RemoveTripUseCaseTest.class.getSimpleName();
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
    public void removeTripUseCaseTest() {

        Trip trip = generateTrip();

        CompletableFuture<String> successFuture = new CompletableFuture<>();
        CompletableFuture<RequestError> errorFuture = new CompletableFuture<>();

        useCaseComponent.removeTripUseCase().execute(trip.getTripId(), trip.getVin(), new RemoveTripUseCase.Callback() {
            @Override
            public void onTripRemoved() {
                Log.d(TAG, "RemoveTripUseCaseTest onTripRemoved()");
                successFuture.complete("Success");
                errorFuture.complete(null);
            }

            @Override
            public void onError(@NotNull RequestError error) {
                successFuture.complete(null);
                errorFuture.complete(error);
                Log.i(TAG, "RemoveTripUseCaseTest onError()");
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
                "                    \"phoneTimestamp\": \"1520629854\"\n" +
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
                "                    \"phoneTimestamp\": \"1520629854\"\n" +
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
                "                    \"phoneTimestamp\": \"1520629854\"\n" +
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
                "                    \"phoneTimestamp\": \"1520631928\"\n" +
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
                "                    \"phoneTimestamp\": \"1520631928\"\n" +
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
                "                    \"phoneTimestamp\": \"1520631928\"\n" +
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
