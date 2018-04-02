package com.pitstop.retrofit

import com.google.gson.JsonElement
import com.pitstop.models.trip.Trip
import io.reactivex.Observable
import retrofit2.Response
import retrofit2.http.*
/**
 * Created by Karol Zdebel on 3/15/2018.
 */
interface PitstopTripApi {

    @POST("v1/trip")
    fun store(@Body body: JsonElement): Observable<Response<String>>

    @GET("/v1/trip")
    fun getTripListFromCarVin(@Query("vin") vin: String): Observable<PitstopResponse<List<Trip>>>

    @DELETE("/v1/trip")
    fun deleteTripById(@Query("tripId") tripId: String, @Query("vin") vin: String): Observable<PitstopResponse<String>>

}