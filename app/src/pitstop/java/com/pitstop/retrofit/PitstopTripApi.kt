package com.pitstop.retrofit

import com.pitstop.models.trip.Trip
import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Created by David C. on 9/3/18.
 */
interface PitstopTripApi {

    @GET("/v1/trip")
    fun getTripListFromCarVin(@Query("vin") vin: String): Observable<PitstopResponse<List<Trip>>>

//    @GET("/v1/trip/{tripId}")
//    fun getTrip(@Path("tripId") id: Int): Observable<Response<Trip>>
//
//    @GET("/v1/trip/{tripId}/polyline")
//    fun getTripPolyline(@Path("tripId") id: Int): Observable<List<Location2>>

}