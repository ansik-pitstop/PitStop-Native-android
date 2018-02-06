package com.pitstop.retrofit

import com.pitstop.models.Car
import java.util.*

/**
 * Created by Karol Zdebel on 10/26/2017.
 */

data class PitstopResponse<T>(val response: T)

data class PitstopResult<T>(val results: T)

data class Token(val accessToken: String)

data class CarList(val data: List<Car>, val emptyData: Object)

data class PredictedService(val predictedDate: Date, val confidenceInterval: Int
                                ,   val confidenceLevel: Double, val nextServiceMileage: Int)

//
//@Parcelize
//data class Car(
//        val _id: Int,
//        val vin: String,
//        val year: Int,
//        val make: String,
//        val model: String,
//        val trim: String,
//        val engine: String,
//        val tankSize: String?,
//        val userId: Int,
//        val cityMileage: String?,
//        val highwayMileage: String?,
//        val baseMileage: Int,
//        val totalMileage: Int,
//        val salesperson: String?
//):Parcelable