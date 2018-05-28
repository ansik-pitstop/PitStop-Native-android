package com.pitstop.models

/**
 * Created by Karol Zdebel on 5/28/2018.
 */
data class PendingUpdate(val vin: String, val type: String, val value: String, val timestamp: Long){
    companion object {
        const val CAR_MILEAGE_UPDATE = "car_mileage_update"
    }
}