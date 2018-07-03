package com.pitstop.retrofit;

import com.google.gson.Gson;
import com.pitstop.models.sensor_data.DataPoint;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.reactivex.schedulers.Schedulers;

import static junit.framework.Assert.assertTrue;

/**
 * Created by Karol Zdebel on 3/15/2018.
 */

public class PitstopTripApiTest {

    @Test
    public void storeTripTest(){
        System.out.println("running storeTripTest");

        Gson gson = new Gson();

        CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();

        String VIN = "1GB0CVCL7BF147611";

        List<List<DataPoint>> data = new ArrayList<>();
        data.add(getRandomLocationDataPoint(VIN,false));
        data.add(getEndTripDataPoint(VIN));

        System.out.println("body: "+gson.toJsonTree(data));
        RetrofitTestUtil.Companion
                .getTripApi()
                .store(gson.toJsonTree(data))
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.io())
                .subscribe(stringResponse -> {
                    completableFuture.complete(true);
                }, errResponse -> {
                    errResponse.printStackTrace();
                    completableFuture.complete(false);
                });
        try{
            assertTrue(completableFuture.get(12000, TimeUnit.MILLISECONDS));
        }catch(InterruptedException | ExecutionException | TimeoutException e){
            e.printStackTrace();
            throw new AssertionError();
        }
    }

    private List<DataPoint> getRandomLocationDataPoint(String vehVin, boolean isTripIndicator){
        Random r = new Random();
        DataPoint latitiude = new DataPoint(DataPoint.ID_LATITUDE,String.valueOf(r.nextDouble()*100));
        DataPoint longitude = new DataPoint(DataPoint.ID_LONGITUDE,String.valueOf(r.nextDouble()*100));
        DataPoint deviceTimestamp = new DataPoint(DataPoint.ID_DEVICE_TIMESTAMP, String.valueOf(Math.abs(r.nextInt()*100000)));
        DataPoint tripId = new DataPoint(DataPoint.ID_TRIP_ID, String.valueOf(Math.abs(r.nextInt()*100000)));
        DataPoint vin = new DataPoint(DataPoint.ID_VIN, vehVin);
        DataPoint tripIndicator = new DataPoint(DataPoint.ID_TRIP_INDICATOR, isTripIndicator? "true":"false");

        List<DataPoint> locationPoint = new ArrayList<>();
        locationPoint.add(latitiude);
        locationPoint.add(longitude);
        locationPoint.add(deviceTimestamp);
        locationPoint.add(tripId);
        locationPoint.add(vin);
        locationPoint.add(tripIndicator);

        return locationPoint;
    }

    private List<DataPoint> getEndTripDataPoint(String vehVin){
        DataPoint startLocation = new DataPoint(DataPoint.ID_START_LOCATION, "Start location");
        DataPoint endLocation = new DataPoint(DataPoint.ID_END_LOCATION, "End location");
        DataPoint startStreetLocation = new DataPoint(DataPoint.ID_START_STREET_LOCATION, "Start street location");
        DataPoint endStreetLocation = new DataPoint(DataPoint.ID_END_STREET_LOCATION, "End street location");
        DataPoint startCityLocation = new DataPoint(DataPoint.ID_START_CITY_LOCATION, "Start city location");
        DataPoint endCityLocation = new DataPoint(DataPoint.ID_END_CITY_LOCATION, "End city location");
        DataPoint startLatitude = new DataPoint(DataPoint.ID_START_LATITUDE, "12.9");
        DataPoint endLatitude = new DataPoint(DataPoint.ID_END_LATITUDE, "13.1");
        DataPoint startLongtitude = new DataPoint(DataPoint.ID_START_LONGTITUDE, "11.2");
        DataPoint endLongitude = new DataPoint(DataPoint.ID_END_LONGITUDE, "11.1");
        DataPoint mileageTrip = new DataPoint(DataPoint.ID_MILEAGE_TRIP, "22.2");
        DataPoint startTimestamp = new DataPoint(DataPoint.ID_START_TIMESTAMP, "10");
        DataPoint endTimestamp = new DataPoint(DataPoint.ID_END_TIMESTAMP, "111");

        List<DataPoint> tripEnd = getRandomLocationDataPoint(vehVin,true);

        tripEnd.add(startLocation);
        tripEnd.add(endLocation);
        tripEnd.add(startStreetLocation);
        tripEnd.add(endStreetLocation);
        tripEnd.add(startCityLocation);
        tripEnd.add(endCityLocation);
        tripEnd.add(startLatitude);
        tripEnd.add(endLatitude);
        tripEnd.add(startLongtitude);
        tripEnd.add(endLongitude);
        tripEnd.add(mileageTrip);
        tripEnd.add(startTimestamp);
        tripEnd.add(endTimestamp);

        return tripEnd;
    }
}
