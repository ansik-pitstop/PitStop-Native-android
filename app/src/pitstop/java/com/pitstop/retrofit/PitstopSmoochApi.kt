package com.pitstop.retrofit

import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.GET

/**
 * Created by Karol Zdebel on 2/27/2018.
 */
interface PitstopSmoochApi {
    @GET("/v1/smoochToken")
    fun getSmoochToken(): Call<JsonObject>
}