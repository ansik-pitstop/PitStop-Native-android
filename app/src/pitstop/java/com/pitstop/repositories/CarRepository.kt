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
        if (car != null){
            val localCarList = List<Car>(1,{car})
            return Observable.just(PitstopResponse(localCarList))
        }else{
            return pitstopCarApi.getCar(vin)
        }
    }

    fun getShopId(carId: Int, callback: Repository.Callback<Int>) {

    }

    fun insert(vin: String, baseMileage: Double, userId: Int, scannerId: String): Observable<PitstopResponse<Car>> {


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
