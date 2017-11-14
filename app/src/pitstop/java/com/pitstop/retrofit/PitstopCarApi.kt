package com.pitstop.retrofit

import com.google.gson.JsonElement
import com.pitstop.models.Car
import io.reactivex.Observable
import retrofit2.Response
import retrofit2.http.*

/**
 * Created by Karol Zdebel on 10/26/2017.
 */
interface PitstopCarApi {

    @GET("car/{carId}")
    fun getCar(@Path("carId") id: Int): Observable<Response<Car>>

    @PUT("v1/car/{carId}")
    fun updateMileage(@Path("carId") id: Int, @Field("totalMileage") mileage: Double): Observable<PitstopResponse<Car>>

    @GET("v1/car")
    fun getCar(@Query("vin") vin: String): Observable<Response<List<Car>>>

    @GET("car")
    fun getUserCars(@Query("userId") userId: Int): Observable<Response<JsonElement>>

    @GET("v1/car")
    fun getCarShopId(@Query("carId") carId: Int): Observable<PitstopResponse<Int>>

    @DELETE("car")
    fun delete(@Query("carId") carId: Int): Observable<PitstopResponse<String>>

    @POST("v1/car")
    fun add(@Field("vin") vin: String, @Field("baseMileage") baseMileage: Double
            , @Field("userId") userId: Int, @Field("scannerId") scannerId: String): Observable<PitstopResponse<Car>>
}