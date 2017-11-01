package com.pitstop.repositories

import android.util.Log
import com.google.gson.JsonIOException
import com.pitstop.BuildConfig
import com.pitstop.database.LocalCarStorage
import com.pitstop.models.Car
import com.pitstop.network.RequestError
import com.pitstop.retrofit.PitstopCarApi
import com.pitstop.utils.NetworkHelper
import io.reactivex.Observable
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*

/**
 * Car repository, use this class to modify, retrieve, and delete car data.
 * Updates data both remotely and locally.
 *
 * Created by Karol Zdebel on 5/26/2017.
 */

class CarRepository(private val localCarStorage: LocalCarStorage
                    , private val networkHelper: NetworkHelper
                    , private val carApi: PitstopCarApi) : Repository {

    private val TAG = javaClass.simpleName

    fun getCarByVin(vin: String, callback: Repository.Callback<Car>) {
        networkHelper.get("car/?vin=" + vin) { response, requestError ->
            if (requestError == null) {

                //No car found
                if (response == null || response == "{}") {
                    callback.onSuccess(null)
                    return@get
                }
                //Create car
                try {
                    val car = Car.createCar(response)
                    localCarStorage.deleteCar(car.id)
                    localCarStorage.storeCarData(car)
                    callback.onSuccess(car)

                } catch (e: JSONException) {
                    e.printStackTrace()
                    callback.onError(RequestError.getUnknownError())
                }

            } else {
                callback.onError(requestError)
            }
        }
    }

    fun getShopId(carId: Int, callback: Repository.Callback<Int>) {
        networkHelper.get("v1/car/shop?carId=" + carId) { response, requestError ->
            if (requestError == null) {
                Log.d(TAG, "getShopId resposne: " + response)
                try {
                    val jsonResponse = JSONObject(response)
                            .getJSONArray("response").getJSONObject(0)
                    callback.onSuccess(jsonResponse.getInt("shopId"))
                } catch (e: JSONException) {
                    e.printStackTrace()
                    callback.onError(RequestError.getUnknownError())
                }

            }
        }
    }

    fun insert(vin: String, baseMileage: Double, userId: Int, scannerId: String, callback: Repository.Callback<Car>) {
        //Insert to backend
        val body = JSONObject()

        try {
            body.put("vin", vin)
            body.put("baseMileage", baseMileage)
            body.put("userId", userId)
            body.put("scannerId", scannerId)
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        networkHelper.post("car", { response, requestError ->
            try {
                if (requestError == null) {
                    var car: Car? = null
                    try {
                        car = Car.createCar(response)
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        callback.onError(RequestError.getUnknownError())
                        return@post
                    }

                    localCarStorage.deleteCar(car!!.id)
                    localCarStorage.storeCarData(car)
                    callback.onSuccess(car)
                } else {
                    callback.onError(requestError)
                }
            } catch (e: JsonIOException) {
                callback.onError(RequestError.getUnknownError())
            }
        }, body)

    }

    fun update(car: Car, callback: Repository.Callback<Any>) {
        val body = JSONObject()

        try {
            body.put("carId", car.id)
            body.put("totalMileage", car.totalMileage)
            body.put("shopId", car.shopId)
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        networkHelper.put("car", { response, requestError ->
            if (requestError == null) {
                localCarStorage.updateCar(car)
                callback.onSuccess(response)
            } else {
                callback.onError(requestError)
            }
        }, body)
    }

    fun getCarsByUserId(userId: Int, callback: Repository.Callback<List<Car>>) {

//        val localResponse = Observable.just(Response(localCarStorage.allCars,null,true))
       // return Observable.concat(localResponse, )
        if (!localCarStorage.allCars.isEmpty()) {
            callback.onSuccess(localCarStorage.allCars)
            return
        }
        networkHelper.getCarsByUserId(userId) { response, requestError ->

            if (requestError == null && response != null) {
                val cars = ArrayList<Car>()

                //Return empty list if no cars returned
                if (response == "{}") {
                    callback.onSuccess(cars)
                    return@getCarsByUserId
                }

                var carsJson = JSONArray()
                try {
                    carsJson = JSONArray(response)
                } catch (e: JSONException) {

                }

                for (i in 0 until carsJson.length()) {
                    try {
                        //Todo: remove correcting shop id below
                        cars.add(Car.createCar(carsJson.getString(i)))
                        if (cars[i].shopId == 0)
                            if (BuildConfig.BUILD_TYPE == BuildConfig.BUILD_TYPE_BETA || BuildConfig.DEBUG)
                                cars[i].shopId = 1
                            else
                                cars[i].shopId = 19
                    } catch (e: Exception) {

                    }

                }
                localCarStorage.deleteAllCars()
                localCarStorage.storeCars(cars)
                callback.onSuccess(cars)
            } else {
                callback.onError(requestError)
            }
        }
    }

    operator fun get(id: Int): Observable<Response<Car>> {

        val local = Observable.just(Response(localCarStorage.getCar(id),true))
        val remote = carApi.getCar(id)
                .map { pitstopResponse -> Response(pitstopResponse.response, false) }
        return Observable.concat(local,remote)
//
//        if (localCarStorage.getCar(id) != null) {
//            callback.onSuccess(localCarStorage.getCar(id))
//            return
//        }
//        networkHelper.getCarsById(id) { response, requestError ->
//            try {
//                if (requestError == null) {
//                    val car = Car.createCar(response)
//                    if (car.shopId == 0)
//                    //Todo: remove correcting shopId below
//                        if (BuildConfig.BUILD_TYPE == BuildConfig.BUILD_TYPE_BETA || BuildConfig.DEBUG)
//                            car.shopId = 1
//                        else
//                            car.shopId = 19
//
//                    localCarStorage.deleteCar(car.id)
//                    localCarStorage.storeCarData(car)
//                    callback.onSuccess(car)
//                } else {
//                    callback.onError(requestError)
//                }
//            } catch (e: JSONException) {
//                callback.onError(RequestError.getUnknownError())
//            }
//        }
    }

    fun delete(carId: Int, callback: Repository.Callback<Any>) {
        networkHelper.deleteUserCar(carId) { response, requestError ->
            try {
                if (requestError == null) {
                    localCarStorage.deleteCar(carId)
                    callback.onSuccess(response)
                } else {
                    callback.onError(requestError)
                }
            } catch (e: JsonIOException) {
                callback.onError(RequestError.getUnknownError())
            }
        }
    }
}
