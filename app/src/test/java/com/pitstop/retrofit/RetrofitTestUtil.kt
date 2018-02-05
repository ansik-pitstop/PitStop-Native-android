package com.pitstop.retrofit

import com.google.gson.JsonObject
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Created by Karol Zdebel on 2/2/2018.
 */
class RetrofitTestUtil {
    companion object {
        fun getAppointmentApi(): PitstopAppointmentApi = Retrofit.Builder()
                .baseUrl(getBaseURL())
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(getHttpClient())
                .build()
                .create(PitstopAppointmentApi::class.java)

        private fun getAccessToken(): String{
            val username = "unit_test@test.com"
            val password = "testing123"
            val installationId = "0fd351e2-01db-416a-b58e-a4985d36669d"
            val authApi = Retrofit.Builder()
                    .baseUrl(getBaseURL())
                    .addConverterFactory(GsonConverterFactory.create())
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .client(getNoAuthHttpClient())
                    .build()
                    .create(PitstopAuthApi::class.java)
            val jsonObject = JsonObject()
            jsonObject.addProperty("username",username)
            jsonObject.addProperty("password",password)
            jsonObject.addProperty("installationId",installationId)
            val response = authApi.login(jsonObject).execute()
            if (response.isSuccessful) return response.body()!!["accessToken"].asString
            else{
                throw Exception()
            }
        }

        private fun getBaseURL(): String = "http://staging.api.getpitstop.io:10010/"

        private fun getHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                    .addInterceptor { chain ->
                        val original = chain.request()

                        val accessToken = getAccessToken()
                        System.out.println("access token: "+accessToken)
                        val builder = original.newBuilder()
                                .header("client-id", getClientId())
                                .header("Content-Type", "application/json")
                                .header("Authorization", "Bearer " + accessToken)

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

        private fun getClientId() = "DINCPNWtqjjG69xfMWuF8BIJ8QjwjyLwCq36C19CkTIMkFnE6zSxz7Xoow0aeq8M6Tlkybu8gd4sDIKD"
    }
}