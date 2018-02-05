package com.pitstop.repositories

import android.util.Log
import com.google.gson.JsonObject
import com.pitstop.database.LocalAppointmentStorage
import com.pitstop.models.Appointment
import com.pitstop.retrofit.PitstopAppointmentApi
import io.reactivex.Observable
import java.text.SimpleDateFormat

/**
 * Created by Karol Zdebel on 2/2/2018.
 */
class AppointmentRepository(private val localAppointmentStorage: LocalAppointmentStorage
                            , private val appointmentApi: PitstopAppointmentApi): Repository{

    private val tag = javaClass.simpleName

    fun getAppointment(id: Int): Observable<Appointment> {
        Log.d(tag,String.format("getAppointment() id: %d",id))
        return Observable.just(Appointment())
    }

    fun requestAppointment(userId: Int, carId: Int, appointment: Appointment): Observable<Boolean>{
        Log.d(tag,String.format("requestAppointment() userId: %d, carID: %d, appointment: %s"
                , userId, carId, appointment))
        val body: JsonObject = JsonObject()
        val options: JsonObject = JsonObject()
        options.addProperty("state",appointment.state)
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        options.addProperty("appointmentDate",simpleDateFormat.format(appointment.date))
        body.add("options",options)
        body.addProperty("userId",userId)
        body.addProperty("carId",carId)
        body.addProperty("shopId",appointment.shopId)
        body.addProperty("comments",appointment.comments)
        return appointmentApi.requestService(body).map { next -> next.isSuccessful }
    }

    fun getAllAppointments(carId: Int): Observable<List<Appointment>>{
        Log.d(tag,String.format("getAllAppointments() carId: %d",carId))
        return Observable.just(ArrayList())
    }

}