package com.pitstop.repositories;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;

import com.pitstop.database.LocalAppointmentStorage;
import com.pitstop.models.Appointment;
import com.pitstop.retrofit.PitstopAppointmentApi;
import com.pitstop.retrofit.PredictedService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/**
 * Created by Karol Zdebel on 2/5/2018.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class AppointmentRepositoryTest {

    private AppointmentRepository appointmentRepository;

    @Before
    public void setup(){
        System.out.println("running setup()");
        Context context = InstrumentationRegistry.getTargetContext();
        PitstopAppointmentApi api = RetrofitTestUtil.Companion.getAppointmentApi();
        LocalAppointmentStorage local = new LocalAppointmentStorage(context);
        appointmentRepository = new AppointmentRepository(local,api);
    }

    @Test
    public void getAppointmentTest(){
        System.out.println("running getAppointmentTest()");
    }

    @Test
    public void getAllAppointmentsTest(){
        System.out.println("running getAllAppointmentsTet()");

        //Input
        CompletableFuture<List<Appointment>> future = new CompletableFuture<>();
        int carId = 5622;
        appointmentRepository.getAllAppointments(carId)
                .doOnNext(next -> {
                    System.out.println("Got appointments: "+next);
                    future.complete(next);
                })
                .subscribe();

        try{
            assertTrue(future
                    .get(2000, java.util.concurrent.TimeUnit.MILLISECONDS).size() > 0);
        }catch( InterruptedException | ExecutionException | TimeoutException e){
            e.printStackTrace();
        }
    }

    @Test
    public void getPredictedServiceTest(){
        System.out.println("running getPredictedServiceTest()");

        //Input
        int carId = 5622;
        CompletableFuture<PredictedService> future = new CompletableFuture<>();
        appointmentRepository.getPredictedService(carId)
                .doOnNext(next -> future.complete(next))
                .subscribe();

        try{
            assertNotNull(future.get(2000, java.util.concurrent.TimeUnit.MILLISECONDS));
        }catch( InterruptedException | ExecutionException | TimeoutException e){
            e.printStackTrace();
        }

    }

    @Test
    public void requestAppointmentTest(){
        System.out.println("running requestAppointmentTest");

        //Input
        int carId = 5622;
        int userId = 1559;
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Appointment app = generateAppointment();
        appointmentRepository.requestAppointment(userId,carId,app)
                .doOnNext(next -> future.complete(next))
                .subscribe();

        try{
            assertTrue(future.get(2000, java.util.concurrent.TimeUnit.MILLISECONDS));
        }catch( InterruptedException | ExecutionException | TimeoutException e){
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
}
