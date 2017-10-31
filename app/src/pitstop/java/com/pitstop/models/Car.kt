package com.pitstop.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * Created by Karol Zdebel on 10/31/2017.
 */

@Parcelize
data class Car(
        val _id: Int,
        val vin: String,
        val year: Int,
        val make: String,
        val model: String,
        val trim: String,
        val engine: String,
        val tankSize: String?,
        val userId: Int,
        val cityMileage: String?,
        val highwayMileage: String?,
        val baseMileage: Int,
        val totalMileage: Int,
        val salesperson: String?,
        val isCurrentCar: Boolean,
        val scannerId: String,
        val shopId: Int
): Parcelable