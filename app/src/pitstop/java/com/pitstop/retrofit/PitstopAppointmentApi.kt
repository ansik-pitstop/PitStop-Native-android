package com.pitstop.retrofit

import com.google.gson.JsonObject
import com.pitstop.models.Appointment
import io.reactivex.Observable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Created by Karol Zdebel on 2/2/2018.
 */
interface PitstopAppointmentApi {

    @POST("utility/serviceRequest")
    fun requestService(@Body body: JsonObject): Observable<Appointment>

    @GET("car/{carId}/appointments")
    fun getAppointments(@Path("carId") id: Int): Observable<List<Appointment>>

}