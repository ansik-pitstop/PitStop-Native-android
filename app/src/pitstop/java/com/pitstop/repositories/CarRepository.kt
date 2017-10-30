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
                .subscribe { response -> localCarStorage.storeCarData(response.response[0]) }
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
            .subscribe { response -> localCarStorage.storeCarData(response.response) }
        return o
    }

    fun update(car: Car): Observable<PitstopResponse<Car>> {

    }

    fun getCarsByUserId(userId: Int): Observable<PitstopResponse<List<Car>>> {

    }

    operator fun get(id: Int): Observable<PitstopResponse<Car>> {
        val local = Observable.just(
                PitstopResponse<Car>(localCarStorage.getCarRetrofit(id)))
        return Observable
                .concat(local, pitstopCarApi.getCar(id))
    }

    fun delete(carId: Int): Observable<PitstopResponse<String>> {

    }

}
