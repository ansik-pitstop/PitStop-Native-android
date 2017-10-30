package com.pitstop.retrofit

import io.reactivex.Observable
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * Created by Karol Zdebel on 10/26/2017.
 */
interface PitstopCarApi {
    @GET("v1/car")
    fun getCar(@Url id: Int): Observable<PitstopResponse<Car>>

    @GET("v1/car")
    fun getCar(@Query("vin") vin: String): Observable<PitstopResponse<Car>>

    @GET("v1/car")
    fun getUserCars(@Query("userId") userId: Int): Observable<PitstopResponse<List<Car>>>

    @GET("v1/car")
    fun getCarShopId(@Query("carId") carId: Int): Observable<PitstopResponse<Int>>

    @DELETE("car")
    fun delete(@Query("carId") carId: Int): Observable<PitstopResponse<Int>>
}