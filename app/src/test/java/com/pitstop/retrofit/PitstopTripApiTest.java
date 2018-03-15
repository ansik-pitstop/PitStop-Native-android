package com.pitstop.retrofit;

import com.pitstop.models.trip.DataPoint;
import com.pitstop.models.trip.LocationDataPoint;

import org.junit.Test;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static junit.framework.Assert.assertTrue;

/**
 * Created by Karol Zdebel on 3/15/2018.
 */

public class PitstopTripApiTest {

    @Test
    public void storeTripTest(){
        System.out.println("running storeTripTest");

        //Input
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        String VIN = "";

        LocationDataPoint locationDataPoint = new LocationDataPoint()


        try{
            assertTrue(future.get(10000, java.util.concurrent.TimeUnit.MILLISECONDS));
        }catch(InterruptedException | ExecutionException | TimeoutException e){
            e.printStackTrace();
        }
    }

    private LocationDataPoint getRandomLocationDataPoint(String vehVin){
        Random r = new Random();
        DataPoint latitiude = new DataPoint(DataPoint.ID_LATITUDE,String.valueOf(r.nextDouble()*100));
        DataPoint longitude = new DataPoint(DataPoint.ID_LATITUDE,String.valueOf(r.nextDouble()*100));
        DataPoint deviceTimestamp = new DataPoint(DataPoint.ID_DEVICE_TIMESTAMP, String.valueOf(r.nextInt()*100000));
        DataPoint tripId = new DataPoint(DataPoint.ID_TRIP_ID, String.valueOf(r.nextInt()*100000));
        DataPoint vin = new DataPoint(DataPoint.ID_VIN, vehVin);
        DataPoint tripIndicator = new DataPoint(DataPoint.ID_TRIP_INDICATOR, "false");
        return new LocationDataPoint(longitude,latitiude,tripIndicator,deviceTimestamp,tripId,vin
                ,null,null,null,null
                ,null,null,null,null,null
                ,null,null,null,null);
    }
}
