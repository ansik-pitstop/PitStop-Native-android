package com.pitstop.retrofit

import com.google.gson.JsonObject
import com.pitstop.models.User
import io.reactivex.Observable
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Created by Karol Zdebel on 10/26/2017.
 */
interface PitstopAuthApi {

    @POST("login/refresh")
    fun refreshAccessToken(@Body refreshToken: JsonObject): Call<Token>

    @POST("login")
    fun login(@Body login: JsonObject): Observable<LoginResponse>

    @POST("login")
    fun loginSync(@Body login: JsonObject): Call<JsonObject>

    @POST("login/social")
    fun loginSocial(@Body login: JsonObject): Observable<LoginResponse>

    @POST("user")
    fun signUp(@Body user: JsonObject): Observable<User>

    @POST("user/{userId}/password")
    fun changePassword(@Path("userId") userId: Int, @Body change: JsonObject): Observable<ChangePasswordResponse>

}