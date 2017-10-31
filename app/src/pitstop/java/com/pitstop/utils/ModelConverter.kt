package com.pitstop.utils

/**
 * Created by Karol Zdebel on 10/31/2017.
 */
class ModelConverter {
    fun generateCar(retrofitCar: com.pitstop.retrofit.Car, currentCarId: Int, scannerId: String)
            = com.pitstop.models.Car(retrofitCar._id, retrofitCar.vin, retrofitCar.year
            , retrofitCar.make, retrofitCar.model, retrofitCar.trim, retrofitCar.engine
            , retrofitCar.tankSize, retrofitCar.userId, retrofitCar.cityMileage
            , retrofitCar.highwayMileage, retrofitCar.baseMileage, retrofitCar.totalMileage
            , retrofitCar.salesperson, currentCarId == retrofitCar._id, scannerId)
}