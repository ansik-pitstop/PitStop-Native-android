package com.pitstop.retrofit

import com.pitstop.models.snapToRoad.SnappedPoint
import io.reactivex.Observable
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Created by David C. on 16/3/18.
 */
interface GoogleSnapToRoadApi {

    @GET("/v1/snapToRoads")
    fun getSnapToRoadFromLatLng(@Query("path") parameters: String
                                , @Query("interpolate") interpolate: String
                                , @Query("key") apiKey: String)
            : Observable<SnapToRoadResponse<List<SnappedPoint>>>

    @GET("/v1/snapToRoads")
    fun getSnapToRoadFromLatLngCall(@Query("path") parameters: String
                                , @Query("interpolate") interpolate: String
                                , @Query("key") apiKey: String)
            : Call<SnapToRoadResponse<List<SnappedPoint>>>
}