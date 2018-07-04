package com.pitstop.retrofit

import com.google.gson.JsonObject
import com.pitstop.models.issue.CarIssue
import com.pitstop.models.issue.UpcomingIssue
import io.reactivex.Observable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Created by Karol Zdebel on 7/4/2018.
 */
interface PitstopServiceApi {

    @GET("car/{carId}/issues?type=active")
    fun getCurrentServices(@Path("carId") carId: Int): Observable<PitstopResult<List<CarIssue>>>

    @GET("car/{carId}/issues?type=history")
    fun getDoneServices(@Path("carId") carId: Int): Observable<PitstopResult<List<CarIssue>>>

    @GET("car/{carId}/issues?type=upcoming")
    fun getUpcomingServices(@Path("carId") carId: Int): Observable<PitstopResult<List<PitstopIssuesResponse<UpcomingIssue>>>>

    @PUT("issue")
    fun updateService(@Body body: JsonObject): Observable<CarIssue>
}