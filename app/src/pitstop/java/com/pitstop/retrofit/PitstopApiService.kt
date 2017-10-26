package com.pitstop.retrofit

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Url

/**
 * Created by Karol Zdebel on 10/26/2017.
 */
interface PitstopApiService {
    @GET
    fun getCar(@Url id: String): Call<PitstopResponse<Car>>
}