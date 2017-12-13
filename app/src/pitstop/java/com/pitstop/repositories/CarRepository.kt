package com.pitstop.repositories

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonIOException
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.pitstop.BuildConfig
import com.pitstop.database.LocalCarStorage
import com.pitstop.models.Car
import com.pitstop.models.DebugMessage
import com.pitstop.network.RequestError
import com.pitstop.retrofit.PitstopCarApi
import com.pitstop.utils.Logger
import com.pitstop.utils.NetworkHelper
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import org.json.JSONException
import org.json.JSONObject



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
                    Logger.getInstance()!!.logException(tag, e, DebugMessage.TYPE_REPO)
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
                    val jsonArray = JSONObject(response)
                            .getJSONArray("response")
                    if (jsonArray.length() > 0){
                        callback.onSuccess(jsonArray.getJSONObject(0).getInt("shopId"))
                    }else{
                        if ((BuildConfig.DEBUG || BuildConfig.BUILD_TYPE == BuildConfig.BUILD_TYPE_BETA)) {
                            callback.onSuccess(1)
                        } else if (BuildConfig.BUILD_TYPE == BuildConfig.BUILD_TYPE_RELEASE) {
                            callback.onSuccess(19)
                        }
                    }
                } catch (e: JSONException) {
                    Logger.getInstance()!!.logException(tag, e, DebugMessage.TYPE_REPO)
                    e.printStackTrace()
                    callback.onError(RequestError.getUnknownError())
                }

            }
        }
    }

    fun insert(vin: String, baseMileage: Double, userId: Int, scannerId: String?, callback: Repository.Callback<Car>) {
        //Insert to backend
        val body = JSONObject()

        try {
            body.put("vin", vin)
            body.put("baseMileage", baseMileage)
            body.put("userId", userId)
            body.put("scannerId", scannerId)
        } catch (e: JSONException) {
            Logger.getInstance()!!.logException(tag, e, DebugMessage.TYPE_REPO)
        }

        networkHelper.post("car", { response, requestError ->
            try {
                if (requestError == null) {
                    var car: Car? = null
                    try {
                        car = Car.createCar(response)
                    } catch (e: JSONException) {
                        Logger.getInstance()!!.logException(tag, e, DebugMessage.TYPE_REPO)
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
                Logger.getInstance()!!.logException(tag, e, DebugMessage.TYPE_REPO)
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
            Logger.getInstance()!!.logException(tag, e, DebugMessage.TYPE_REPO)
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

        val remote: Observable<RepositoryResponse<List<Car>>> = carApi.getUserCars(userId).map{ carListResponse ->
            if (carListResponse.body() == null || ( (carListResponse.body() as JsonElement).isJsonObject
                    && (carListResponse.body() as JsonObject).size() == 0) ){
                return@map RepositoryResponse(emptyList<Car>(),false)
            }else{
                val gson = Gson()
                val listType = object : TypeToken<List<Car>>() {}.type
                val carList: List<Car> = gson.fromJson(carListResponse.body(),listType)
                carList.filter { it.shopId == 0 }
                        .forEach {
                            if (BuildConfig.DEBUG || BuildConfig.BUILD_TYPE == BuildConfig.BUILD_TYPE_BETA)
                                it.shopId = 1
                            else it.shopId = 19
                        }
                return@map RepositoryResponse(carList,false)
            }}

        remote.subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .doOnNext({next ->
                    if (next == null ) return@doOnNext
                    Log.d(tag,"remote.cache() local store update cars: "+next.data)
                    localCarStorage.deleteAndStoreCars(next.data)
                }).onErrorReturn { err ->
                    Log.d(tag,"getCarsByUserId() remote error: $err err cause: {${err.cause}}")
                    RepositoryResponse(null,false)
                }
                .subscribe()

        return Observable.concat(localResponse,remote.cache())
    }

    operator fun get(id: Int): Observable<RepositoryResponse<Car>> {
        Log.d(tag,"get() id: $id")
        val local = Observable.just(RepositoryResponse(localCarStorage.getCar(id),true))
        val remote = carApi.getCar(id)

        remote.map{ carListResponse -> RepositoryResponse(carListResponse.body(),false) }
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .doOnNext({next ->
                    if (next.data == null ) return@doOnNext
                    Log.d(tag,"remote.cache() local store update cars: "+next.data)
                    if (next.data.shopId == 0)
                        if (BuildConfig.DEBUG || BuildConfig.BUILD_TYPE.equals(BuildConfig.BUILD_TYPE_BETA))
                            next.data.shopId = 1
                        else next.data.shopId = 19

                    localCarStorage.deleteAndStoreCar(next.data)
                }).onErrorReturn { err ->
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
                Logger.getInstance()!!.logException(tag, e, DebugMessage.TYPE_REPO)
                callback.onError(RequestError.getUnknownError())
            }
        }
    }
}
