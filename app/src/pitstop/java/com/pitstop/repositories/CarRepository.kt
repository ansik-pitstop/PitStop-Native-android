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
import io.reactivex.schedulers.Schedulers
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Response

/**
 * Car repository, use this class to modify, retrieve, and delete car data.
 * Updates data both remotely and locally.
 *
 * Created by Karol Zdebel on 5/26/2017.
 */

class CarRepository(private val localCarStorage: LocalCarStorage
                    , private val networkHelper: NetworkHelper
                    , private val carApi: PitstopCarApi) : Repository {

    private val tag = javaClass.simpleName

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
                Log.d(tag, "getShopId resposne: " + response)
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
        Log.d(tag,"update() car: $car")
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

    fun getCarsByUserId(userId: Int): Observable<RepositoryResponse<List<Car>>> {
        Log.d(tag,"getCarsByUserId() userId: $userId")

        val localResponse = Observable.just(RepositoryResponse(localCarStorage.allCars,true)).map { next ->
            Log.d(tag,"remote.replay() next: $next")
            next.data
                .orEmpty()
                .filter { it.shopId == 0 }
                .forEach {
                    if (BuildConfig.DEBUG || BuildConfig.BUILD_TYPE == BuildConfig.BUILD_TYPE_BETA)
                        it.shopId = 1
                    else it.shopId = 19
                }
            next
        }
        val remote: Observable<Response<List<Car>>> = carApi.getUserCars(userId)

        remote.map{ carListResponse -> RepositoryResponse(carListResponse.body(),false) }
            .doOnNext({next ->
                if (next == null ) return@doOnNext
                Log.d(tag,"remote.cache() local store update cars: "+next.data)
                localCarStorage.deleteAllCars()
                localCarStorage.storeCars(next.data)
        }).subscribeOn(Schedulers.io())
        .observeOn(Schedulers.computation())
        .onErrorReturn { err ->
            Log.d(tag,"getCarsByUserId() remote error: $err")
            RepositoryResponse(null,false)
        }
        .subscribe()

        val retRemote = remote.cache()
                .map { next ->
                    Log.d(tag,"remote.replay() next: $next")
                    next.body()
                        .orEmpty()
                        .filter { it.shopId == 0 }
                        .forEach {
                            if (BuildConfig.DEBUG || BuildConfig.BUILD_TYPE == BuildConfig.BUILD_TYPE_BETA)
                                it.shopId = 1
                            else it.shopId = 19
                        }
                    RepositoryResponse(next.body(),false)
                }
        return Observable.concat(localResponse,retRemote)
    }

    operator fun get(id: Int): Observable<RepositoryResponse<Car>> {
        Log.d(tag,"get() id: $id")
        val local = Observable.just(RepositoryResponse(localCarStorage.getCar(id),true))
        val remote = carApi.getCar(id)

        remote.map{ carListResponse -> RepositoryResponse(carListResponse.body(),false) }
                .doOnNext({next ->
                    if (next.data == null ) return@doOnNext
                    Log.d(tag,"remote.cache() local store update cars: "+next.data)
                    if (next.data.shopId == 0)
                        if (BuildConfig.DEBUG || BuildConfig.BUILD_TYPE.equals(BuildConfig.BUILD_TYPE_BETA))
                            next.data.shopId = 1
                        else next.data.shopId = 19

                    localCarStorage.deleteCar(next.data.id)
                    localCarStorage.storeCarData(next.data)
                }).subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation())
                .onErrorReturn { err ->
                    Log.d(tag,"getCarsByUserId() remote error: $err")
                    RepositoryResponse(null,false)
                }
                .subscribe()

        val retRemote = remote.cache().map({pitstopResponse ->
            val carList = pitstopResponse.body()
            if (carList != null){

                //Fix shopId if it's 0
                if (carList.shopId == 0)
                    if (BuildConfig.DEBUG || BuildConfig.BUILD_TYPE.equals(BuildConfig.BUILD_TYPE_BETA))
                        carList.shopId = 1
                    else carList.shopId = 19

            }
            RepositoryResponse(carList, false)
        })

        return Observable.concat(local,retRemote)
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
