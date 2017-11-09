package com.pitstop.retrofit

import retrofit2.Call
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Created by Karol Zdebel on 10/26/2017.
 */
interface PitstopAuthApi {

    @POST("login/refresh")
    fun refreshAccessToken(@Query("refreshToken") refreshToken: String): Call<PitstopResponse<Token>>

}