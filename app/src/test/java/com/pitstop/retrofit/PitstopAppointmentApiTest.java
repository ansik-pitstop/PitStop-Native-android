package com.pitstop.retrofit;

import com.google.gson.JsonObject;
import com.pitstop.models.Appointment;

import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import io.reactivex.Observable;
import retrofit2.Response;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/**
 * Created by Karol Zdebel on 2/2/2018.
 */

public class PitstopAppointmentApiTest {

    @Test
    public void requestServiceTest(){
        System.out.println("running requestServiceTest");

        //Input
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        int userIn = 1559;
        int carIdIn = 5622;

        requestService(generateAppointment(),userIn,carIdIn)
                .doOnNext( next -> future.complete(next.isSuccessful()))
                .subscribe();

        try{
            assertTrue(future.get(10000, java.util.concurrent.TimeUnit.MILLISECONDS));
        }catch(InterruptedException | ExecutionException | TimeoutException e){
            e.printStackTrace();
        }
    }

    @Test
    public void getAllAppointmentsTest(){
        System.out.println("running getAllAppointmentsTest");

        //Input
        CompletableFuture<List<Appointment>> future = new CompletableFuture<>();
        int userIn = 1559;
        int carIdIn = 5622;

        //Input
        int numOfAppointmentsToTest = 4;
        List<Appointment> appointmentsIn = new ArrayList<>();
        for (int i=0;i<numOfAppointmentsToTest;i++){
            Appointment generatedAppointment = generateAppointment();
            appointmentsIn.add(generatedAppointment);
            requestService(generatedAppointment,userIn,carIdIn)
                    .doOnNext( next -> System.out.println("request service success: "+next.isSuccessful()))
                    .subscribe();
        }
        System.out.println("Requested appointments: "+appointmentsIn);
        getAllAppointments(carIdIn).doOnNext(next -> {
            future.complete(next);
        }).doOnError(err -> System.out.println("error: "+err) )
        .subscribe();

        try{
            List<Appointment> futureApps
                    = future.get(2000, java.util.concurrent.TimeUnit.MILLISECONDS);
            for (Appointment a: appointmentsIn){
                assertTrue(futureApps.contains(a));
            }
        }catch(InterruptedException | ExecutionException | TimeoutException e){
            e.printStackTrace();
        }

    }

    @Test
    public void getPredictedServiceTest(){
        System.out.println("running getPredictedServiceTest()");

        //Input
        CompletableFuture<PredictedService> future = new CompletableFuture<>();
        int carIdIn = 5622;

        getPredictedService(carIdIn)
                .doOnNext(next ->{
                    System.out.println("predicted service date response: "+next);
                    future.complete(next);
                })
                .subscribe();

        try{
            assertNotNull(future.get(2000, java.util.concurrent.TimeUnit.MILLISECONDS));
        }catch(TimeoutException | InterruptedException | ExecutionException e){
            e.printStackTrace();
        }
    }

    //Generates appointment with random date (usually in the 1970s)
    //Warning: shopId is usually by default set to the cars shop on the backend or seems to be the case
    private Appointment generateAppointment(){
        String state = "tentative";
        double randTime = 1522857600+(Math.random()*9857600);
        Date date = new Date((int)randTime);
        String comments = "john"+randTime;
        int shopId = 2;
        return new Appointment(shopId,state,date,comments);
    }

    private Observable<List<Appointment>> getAllAppointments(int carId){
        return RetrofitTestUtil.Companion.getAppointmentApi()
                .getAppointments(carId).map(result -> {
                    System.out.println("result: "+result);
                    return result.getResults();
                }).onErrorReturn(err -> {
                    System.out.println("err: "+err);
                    return new ArrayList<>();
                });
    }

    private Observable<Response<JsonObject>> requestService(Appointment app, int userId, int carId){
        JsonObject body = new JsonObject();
        body.addProperty("userId",userId);
        body.addProperty("carId",carId);
        body.addProperty("shopId",app.getShopId());
        body.addProperty("comments",app.getComments());
        JsonObject options = new JsonObject();
        options.addProperty("state",app.getState());
        String stringDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CANADA)
                .format(app.getDate());
        options.addProperty("appointmentDate",stringDate);
        body.add("options",options);
        return RetrofitTestUtil.Companion.getAppointmentApi().requestService(body);
    }

    private Observable<PredictedService> getPredictedService(int carId){
        return RetrofitTestUtil.Companion.getAppointmentApi()
                .getPredictedService(carId)
                .map(result -> result.getResponse());
    }
}
