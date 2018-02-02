package com.pitstop.retrofit

import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Created by Karol Zdebel on 10/26/2017.
 */
interface PitstopAuthApi {

    @POST("login/refresh")
    fun refreshAccessToken(@Body refreshToken: JsonObject): Call<Token>

    @POST("login")
    fun login(@Body login: JsonObject): Call<JsonObject>


}