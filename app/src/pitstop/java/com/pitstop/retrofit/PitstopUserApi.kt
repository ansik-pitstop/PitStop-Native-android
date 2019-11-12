package com.pitstop.retrofit

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import io.reactivex.Observable
import retrofit2.http.*

/**
 * Created by Karol Zdebel on 6/29/2018.
 */
interface PitstopUserApi {

    @PUT("user")
    fun putUser(@Body body: JsonElement): Observable<JsonObject>

    @PATCH("v1/user/{userId}/settings")
    fun patchUserSettings(@Path("userId") userId: Int, @Body body: JsonElement): Observable<JsonObject>

    @POST("user/{userId}/password")
    fun changePassword(@Path("userId") userId: Int, @Body change: JsonObject): Observable<ChangePasswordResponse>
}