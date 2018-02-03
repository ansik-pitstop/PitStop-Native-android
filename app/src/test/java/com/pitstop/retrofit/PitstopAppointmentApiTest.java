package com.pitstop.retrofit;

import com.google.gson.JsonObject;
import com.pitstop.models.Appointment;

import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import io.reactivex.Observable;
import retrofit2.Response;

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
            assertTrue(future.get());
        }catch(InterruptedException | ExecutionException e){
            e.printStackTrace();
        }
    }

    //Todo: Appointments need to be removed
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
            System.out.println("generated appointment: "+generatedAppointment);
            appointmentsIn.add(generatedAppointment);
            requestService(generatedAppointment,userIn,carIdIn)
                    .doOnNext( next -> System.out.println("request service success: "+next.isSuccessful()))
                    .subscribe();
        }

        getAllAppointments(carIdIn).doOnNext(next -> {
            System.out.println("All appointments: "+appointmentsIn);
            future.complete(next);
        }).subscribe();
        try{
            for (Appointment a: future.get()){
                appointmentsIn.contains(a);
            }
            //assertEquals(future.get(),appointmentsIn);
        }catch(InterruptedException | ExecutionException e){
            e.printStackTrace();
        }

    }

    private Appointment generateAppointment(){
        String state = "tentative";
        double randTime = 1522857600+(Math.random()*9857600);
        Date date = new Date((int)randTime);
        String stringDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
        String comments = "john";
        int shopId = 3;
        return new Appointment(shopId,state,stringDate,comments);
    }

    private Observable<List<Appointment>> getAllAppointments(int carId){
        return RetrofitTestUtil.Companion.getAppointmentApi().getAppointments(carId);
    }

    private Observable<Response<JsonObject>> requestService(Appointment app, int userId, int carId){
        JsonObject body = new JsonObject();
        body.addProperty("userId",userId);
        body.addProperty("carId",carId);
        body.addProperty("shopId",app.getShopId());
        JsonObject options = new JsonObject();
        options.addProperty("state",app.getState());
        options.addProperty("appointmentDate",app.getDate());
        body.add("options",options);
        return RetrofitTestUtil.Companion.getAppointmentApi().requestService(body);
    }
}
