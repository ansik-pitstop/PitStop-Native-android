package com.pitstop.retrofit

import com.google.gson.JsonObject
import com.pitstop.models.Appointment
import io.reactivex.Observable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Created by Karol Zdebel on 2/2/2018.
 */
interface PitstopAppointmentApi {

    @POST("utility/serviceRequest")
    fun requestService(@Body body: JsonObject): Observable<Response<JsonObject>>

    @GET("car/{carId}/appointments")
    fun getAppointments(@Path("carId") id: Int): Observable<PitstopResult<List<Appointment>>>

    @GET("v1/car/{carId}/next-service-date")
    fun getPredictedService(@Path("carId")id: Int): Observable<PitstopResponse<PredictedService>>

}