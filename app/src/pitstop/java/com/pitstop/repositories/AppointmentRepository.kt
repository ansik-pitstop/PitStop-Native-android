package com.pitstop.repositories

import android.util.Log
import com.google.gson.JsonObject
import com.pitstop.database.LocalAppointmentStorage
import com.pitstop.models.Appointment
import com.pitstop.retrofit.PitstopAppointmentApi
import com.pitstop.retrofit.PredictedService
import io.reactivex.Observable
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by Karol Zdebel on 2/2/2018.
 */
class AppointmentRepository(private val localAppointmentStorage: LocalAppointmentStorage
                            , private val appointmentApi: PitstopAppointmentApi): Repository{

    private val tag = javaClass.simpleName

//    fun getAppointment(id: Int): Observable<Appointment> {
//        Log.d(tag,String.format("getAppointment() id: %d",id))
//        return Observable.just(Appointment())
//    }

    fun requestAppointment(userId: Int, carId: Int, appointment: Appointment): Observable<Boolean>{
        Log.d(tag,String.format("requestAppointment() userId: %d, carID: %d, appointment: %s"
                , userId, carId, appointment))
        val body = JsonObject()
        body.addProperty("userId", userId)
        body.addProperty("carId", carId)
        body.addProperty("shopId", appointment.shopId)
        body.addProperty("comments", appointment.comments)
        val options = JsonObject()
        options.addProperty("state", appointment.getState())
        val stringDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CANADA)
                .format(appointment.getDate())
        options.addProperty("appointmentDate", stringDate)
        body.add("options", options)
        return appointmentApi.requestService(body).map { next -> next.isSuccessful }
    }

    fun getAllAppointments(carId: Int): Observable<List<Appointment>>{
        Log.d(tag,String.format("getAllAppointments() carId: %d",carId))
        return appointmentApi.getAppointments(carId).map({ result ->
            println("result: " + result)
            localAppointmentStorage.deleteAndStoreAppointments(result.results)
            result.results
        })
    }

    fun getPredictedService(carId: Int): Observable<PredictedService>{
        Log.d(tag,String.format("getPredictedService() carId: %d",carId))
        return appointmentApi.getPredictedService(carId).map { next -> next.response }
    }

}