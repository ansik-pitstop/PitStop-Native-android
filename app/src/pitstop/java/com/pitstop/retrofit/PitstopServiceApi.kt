package com.pitstop.retrofit

import com.google.gson.JsonObject
import com.pitstop.models.issue.CarIssue
import com.pitstop.models.issue.UpcomingIssue
import io.reactivex.Observable
import retrofit2.http.*

/**
 * Created by Karol Zdebel on 7/4/2018.
 */
interface PitstopServiceApi {

    @GET("v1/cars/{carId}/issues")
    fun getCurrentServices(@Path("carId") carId: Int,
                           @Query("limit") limit: Int,
                           @Query("offset") offset: Int,
                           @Query("status") status: String): Observable<PitstopPaginatedData<List<CarIssue>>>

    @GET("car/{carId}/issues?type=history")
    fun getDoneServices(@Path("carId") carId: Int): Observable<PitstopResult<List<CarIssue>>>

    @GET("car/{carId}/issues?type=upcoming")
    fun getUpcomingServices(@Path("carId") carId: Int): Observable<PitstopResult<List<PitstopIssuesResponse<UpcomingIssue>>>>

    @PUT("issue")
    fun markDone(@Body body: JsonObject): Observable<CarIssue>
}