package com.pitstop.retrofit

import com.pitstop.models.trip.Trip
import io.reactivex.Observable
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Created by David C. on 9/3/18.
 */
interface PitstopTripApi {

    @GET("/v1/trip")
    fun getTripListFromCarVin(@Query("vin") vin: String): Observable<PitstopResponse<List<Trip>>>

    @DELETE("/v1/trip")
    fun deleteTripById(@Query("tripId") tripId: String, @Query("vin") vin: String): Observable<PitstopResponse<String>>

}