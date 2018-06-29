package com.pitstop.retrofit

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import io.reactivex.Observable
import retrofit2.http.Body
import retrofit2.http.PUT

/**
 * Created by Karol Zdebel on 6/29/2018.
 */
interface PitstopUserApi {

    @PUT("user")
    fun putUser(@Body body: JsonElement): Observable<JsonObject>
}