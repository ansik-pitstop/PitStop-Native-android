package com.pitstop.retrofit

import com.pitstop.models.trip.Location
import com.pitstop.models.trip.Trip
import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Created by David C. on 9/3/18.
 */
interface PitstopTripApi {

    @GET("/v1/trip/")
    fun getTripList(): Observable<List<Trip>>

    @GET("/v1/trip/{tripId}")
    fun getTrip(@Path("tripId") id: Int): Observable<Trip>

    @GET("/v1/trip/{tripId}/polyline")
    fun getTripPolyline(@Path("tripId") id: Int): Observable<List<Location>>

}