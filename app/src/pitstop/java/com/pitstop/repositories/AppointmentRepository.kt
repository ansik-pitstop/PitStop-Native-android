package com.pitstop.repositories

import com.google.gson.JsonObject
import com.pitstop.database.LocalAppointmentStorage
import com.pitstop.models.Appointment
import com.pitstop.retrofit.PitstopAppointmentApi
import io.reactivex.Observable

/**
 * Created by Karol Zdebel on 2/2/2018.
 */
class AppointmentRepository(private val localAppointmentStorage: LocalAppointmentStorage
                            , private val appointmentApi: PitstopAppointmentApi): Repository{

    fun getAppointment(id: Int): Observable<Appointment> {
        return Observable.just(Appointment())
    }

    fun requestAppointment(userId: Int, carId: Int, appointment: Appointment): Observable<Appointment>{
        val body: JsonObject = JsonObject()
        val options: JsonObject = JsonObject()
        options.addProperty("state",appointment.state)
        options.addProperty("appointmentDate",appointment.date)
        body.add("options",options)
        body.addProperty("userId",userId)
        body.addProperty("carId",carId)
        body.addProperty("shopId",appointment.shopId)
        body.addProperty("comments",appointment.comments)
        return appointmentApi.requestService(body)
    }

    fun getAllAppointments(carId: Int): Observable<List<Appointment>>{
        return Observable.just(ArrayList())
    }

}