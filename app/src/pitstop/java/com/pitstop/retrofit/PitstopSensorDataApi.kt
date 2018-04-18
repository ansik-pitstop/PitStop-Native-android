package com.pitstop.retrofit

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import io.reactivex.Observable
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Created by Karol Zdebel on 4/18/2018.
 */
interface PitstopSensorDataApi {
    @POST("v1/sensor-data")
    fun store(@Body body: JsonElement): Observable<JsonObject>
}