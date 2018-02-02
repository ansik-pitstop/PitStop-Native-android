package com.pitstop.retrofit

import com.google.gson.JsonObject
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Created by Karol Zdebel on 2/2/2018.
 */
object RetrofitTestUtil {

    fun getAppointmentApi(): PitstopAppointmentApi = Retrofit.Builder()
            .baseUrl(getBaseURL())
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .client(getHttpClient())
            .build()
            .create(PitstopAppointmentApi::class.java);

    private fun getAccessToken(): String{
        val username = "unit_test@test.com"
        val password = "testing1234"
        val installationId = "68570721-1b9c-418d-ab6c-f8385b1abe73"
        val authApi = Retrofit.Builder()
                .baseUrl(getBaseURL())
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(getNoAuthHttpClient())
                .build()
                .create(PitstopAuthApi::class.java)
        //Todo get access token below using no auth api
        val jsonObject = JsonObject()
        jsonObject.addProperty("username",username)
        jsonObject.addProperty("password",password)
        jsonObject.addProperty("installationId",installationId)
        val response = authApi.login(jsonObject).execute()
        System.out.println("RetrofitTestUtil() response: "+response.body())
        if (response.isSuccessful) return response.body()!!["accessToken"].asString
        else throw Exception()
    }

    private fun getBaseURL(): String = "http://staging.api.getpitstop.io:10010/"

    private fun getHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val original = chain.request()

                    val builder = original.newBuilder()
                            .header("client-id", getClientId())
                            .header("Content-Type", "application/json")
                            .header("Authorization", "Bearer " + getAccessToken())

                    var response: okhttp3.Response? = null
                    chain.proceed(builder.build())
                }.build()
    }

    private fun getNoAuthHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val original = chain.request()

                    val builder = original.newBuilder()
                            .header("client-id", getClientId())
                            .header("Content-Type", "application/json")

                    chain.proceed(builder.build())
                }
                .build()
    }

    private fun getClientId() = "xrxqVtdLZP9QBa7eXx7ZVizrlFRHqmU5UGzcNfVB"
}