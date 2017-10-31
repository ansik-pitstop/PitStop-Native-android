package com.pitstop.repositories

import android.util.Log

import com.google.gson.JsonIOException
import com.pitstop.R.array.car
import com.pitstop.database.LocalCarStorage
import com.pitstop.retrofit.Car
import com.pitstop.retrofit.PitstopCarApi
import com.pitstop.retrofit.PitstopResponse
import com.pitstop.utils.NetworkHelper

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers

/**
 * Car repository, use this class to modify, retrieve, and delete car data.
 * Updates data both remotely and locally.
 *
 * Created by Karol Zdebel on 5/26/2017.
 */

class CarRepository(private val localCarStorage: LocalCarStorage, private val pitstopCarApi: PitstopCarApi) : Repository {

    private val TAG = javaClass.simpleName

    fun getCarByVin(vin: String): Observable<PitstopResponse<List<Car>>> {
        val car = localCarStorage.getCarByVin(vin)
        return if (car != null){
            val localCarList = List<Car>(1,{car})
            Observable.just(PitstopResponse(localCarList))
        }else{
            val o = pitstopCarApi.getCar(vin)
            o.subscribeOn(Schedulers.io())
                .subscribe { response ->
                    localCarStorage.deleteCar(response.response[0]._id)
                    localCarStorage.storeCarData(response.response[0]) }
            o
        }
    }

    //Todo: store this somewhere locally
    fun getShopId(carId: Int): Observable<PitstopResponse<Int>> {
        return pitstopCarApi.getCarShopId(carId)
    }

    fun insert(vin: String, baseMileage: Double, userId: String, scannerId: String): Observable<PitstopResponse<Car>> {
        val o = pitstopCarApi.add(vin,baseMileage,userId,scannerId)
        o.subscribeOn(Schedulers.io())
            .subscribe { response ->
                localCarStorage.deleteCar(response.response._id)
                localCarStorage.storeCarData(response.response) }
        return o
    }

    fun updateMileage(id: Int, mileage: Double): Observable<PitstopResponse<Car>> {
        val o = pitstopCarApi.updateMileage(id, mileage)
        o.subscribeOn(Schedulers.io())
            .subscribe { response -> localCarStorage.updateCar(response.response) }
    }

    fun getCarsByUserId(userId: Int): Observable<PitstopResponse<List<Car>>> {
        val cars = localCarStorage.getCarsByUserId(userId)
        return if (cars.isEmpty()){
            Observable.just(PitstopResponse(cars))
        }else{
            val o = pitstopCarApi.getUserCars(userId)
            o.subscribeOn(Schedulers.io())
                    .subscribe { response ->
                        localCarStorage.deleteAllCars()
                        localCarStorage.storeCars(response.response)
            }
            o
        }
    }

    operator fun get(id: Int): Observable<PitstopResponse<Car>> {
        val car = localCarStorage.getCar(id)
        return if (car != null){
            Observable.just(PitstopResponse(car))
        }else{
            val o = pitstopCarApi.getCar(id)
            o.subscribeOn(Schedulers.io())
                    .subscribe { response ->
                        localCarStorage.deleteCar(response.response._id)
                        localCarStorage.storeCarData(response.response)
                    }
            o
        }
    }

    fun delete(carId: Int): Observable<PitstopResponse<String>> {
        val o = pitstopCarApi.delete(carId)
        o.subscribeOn(Schedulers.io())
                .subscribe { localCarStorage.deleteCar(carId)}
        return o
    }

}
