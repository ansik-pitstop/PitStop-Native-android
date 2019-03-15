package com.pitstop.repositories

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonIOException
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.pitstop.BuildConfig
import com.pitstop.database.LocalCarStorage
import com.pitstop.database.LocalShopStorage
import com.pitstop.models.Car
import com.pitstop.models.DebugMessage
import com.pitstop.models.PendingUpdate
import com.pitstop.network.RequestError
import com.pitstop.retrofit.PitstopCarApi
import com.pitstop.retrofit.TotalMileage
import com.pitstop.utils.Logger
import com.pitstop.utils.NetworkHelper
import io.reactivex.Observable
import org.json.JSONException
import org.json.JSONObject


/**
 * Car repository, use this class to modify, retrieve, and delete car data.
 * Updates data both remotely and locally.
 *
 * Created by Karol Zdebel on 5/26/2017.
 */

open class CarRepository(private val localCarStorage: LocalCarStorage
                     , private val localShopStorage: LocalShopStorage
                    , private val networkHelper: NetworkHelper
                    , private val carApi: PitstopCarApi) : Repository {

    private val tag = javaClass.simpleName

    fun getCarByVin(userId: String, vin: String, callback: Repository.Callback<Car>) {

        if (vin.isEmpty()) {
            val err = RequestError()
            err.error = "Empty VIN"
            err.message = "VIN cannot be empty."
            callback.onError(err)
            return
        }

        networkHelper.get("v1/car?userId=$userId&vin=$vin") { response, requestError ->
            if (requestError == null) {

                //No car found
                if (response == null || response == "{}") {
                    callback.onSuccess(null)
                    return@get
                }
                //Create car
                //DO NOT save car to local db here since it may not belong to the user
                try {
                    val car = Car.createCar(response)
                    localCarStorage.deleteCar(car.id)
                    localCarStorage.storeCarData(car)
                    callback.onSuccess(car)

                } catch (e: Exception) {
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

            }else{
                callback.onError(requestError)
            }
        }
    }

    fun insert(vin: String, baseMileage: Double, userId: Int
                        , scannerId: String?, callback: Repository.Callback<Car>) {
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

        networkHelper.post("v1/car", { response, requestError ->
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

    fun setCurrent(car: Car) {
        car.isCurrentCar = true
        localCarStorage.deleteAndStoreCar(car)
    }

    fun updateMileage(carId: Int, mileage: Double): Observable<Boolean>{
        return carApi.updateMileage(TotalMileage(mileage,carId))
                .doOnNext({localCarStorage.updateCarMileage(carId, mileage)})
                .map { true }
                .doOnError({
                    localCarStorage.storePendingUpdate(
                            PendingUpdate(carId,PendingUpdate.CAR_MILEAGE_UPDATE,mileage.toString(),System.currentTimeMillis())
                    )})
    }

    //User id is needed here since cars that were cached during the VIN verification process or car adding process may be in the local db
    // which do not belong to this user
    fun getCarsByUserId(userId: Int, type: Repository.DATABASE_TYPE): Observable<RepositoryResponse<List<Car>>> {
        Log.d(tag,"getCarsByUserId() userId: $userId")
        return when (type) {
            Repository.DATABASE_TYPE.LOCAL -> getAllLocal(userId)
            Repository.DATABASE_TYPE.REMOTE -> getAllRemote(userId)
            Repository.DATABASE_TYPE.BOTH -> {
                val list: MutableList<Observable<RepositoryResponse<List<Car>>>> = mutableListOf()
                list.add(getAllLocal(userId))
                list.add(getAllRemote(userId))
                Observable.concatDelayError(list)
            }
        }
    }

    fun get(id: Int, type: Repository.DATABASE_TYPE): Observable<RepositoryResponse<Car>> {
        Log.d(tag,"get() id: $id")
        return when (type) {
            Repository.DATABASE_TYPE.LOCAL -> getLocal(id)
            Repository.DATABASE_TYPE.REMOTE -> getRemote(id)
            Repository.DATABASE_TYPE.BOTH -> {
                val list: MutableList<Observable<RepositoryResponse<Car>>> = mutableListOf()
                list.add(getLocal(id))
                list.add(getRemote(id))
                Observable.concatDelayError(list)
            }
        }
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

    fun sendPendingUpdates(): Observable<List<PendingUpdate>>{
        val pendingUpdates = localCarStorage.getPendingUpdates()
        Log.d(tag,"Got pending updates: $pendingUpdates")
        val observables = arrayListOf<Observable<PendingUpdate>>()
        pendingUpdates.forEach {
            when(it.type){
                (PendingUpdate.CAR_MILEAGE_UPDATE) -> {
                    observables.add(updateMileage(it.id, it.value.toDouble())
                            .doOnNext({ _ -> localCarStorage.removePendingUpdate(it)})
                            .doOnError({ _ -> localCarStorage.removePendingUpdate(it)}) //Remove it because it is stored again in the mileage update anyway
                            .map { _ -> it })
                }
            }
        }
        return if (observables.isEmpty()) Observable.just(arrayListOf())
        else Observable.combineLatestDelayError(observables, { it.asList() as List<PendingUpdate> })
    }

    private fun getAllLocal(userId: Int): Observable<RepositoryResponse<List<Car>>> {
        return Observable.just(RepositoryResponse(localCarStorage.getAllCars(userId),true)).doOnNext({ next ->
            Log.d(tag,"remote.replay() next: $next")
            next.data.orEmpty()
                    .forEach {
                        //Set dealership for local responses
                        val dealership = localShopStorage.getDealership(it.shopId)
                        it.shop = dealership
                    }
        })
    }

    private fun getAllRemote(userId: Int): Observable<RepositoryResponse<List<Car>>> {
        return carApi.getUserCars(userId).map{ carListResponse ->
            if (!carListResponse.isSuccessful || (carListResponse.body() == null || ( (carListResponse.body() as JsonElement).isJsonObject
                            && (carListResponse.body() as JsonObject).size() == 0) )){
                return@map RepositoryResponse(emptyList<Car>(),false)
            }else{
                val gson = Gson()
                val listType = object : TypeToken<List<Car>>() {}.type
                val carList: List<Car> = gson.fromJson(carListResponse.body(),listType)
                return@map RepositoryResponse(carList,false)
            }}.doOnNext {
                it.data?.forEach {
                    if (it.shop != null) {
                        localShopStorage.removeById(it.shopId)
                        localShopStorage.storeDealership(it.shop)
                    }

                    val localCar = localCarStorage.getCar(it.id)
                    if (localCar != null && localCar.isCurrentCar) {
                        it.isCurrentCar = true
                    }
                    localCarStorage.deleteAndStoreCar(it)
                }
            }
    }

    private fun getLocal(id: Int): Observable<RepositoryResponse<Car>>{
        Log.d(tag,"getLocal() id: $id")
        return Observable.just(RepositoryResponse(localCarStorage.getCar(id),true))
                .doOnNext {
                    if (it.data != null){
                        val dealership = localShopStorage.getDealership(it.data.shopId)
                        it.data.shop = dealership
                    }
                }
    }

    private fun getRemote(id: Int): Observable<RepositoryResponse<Car>>{
        Log.d(tag,"getRemote() id: $id")
        val remote = carApi.getCar(id)

        return remote.map{ carListResponse -> RepositoryResponse(carListResponse,false) }
                .doOnNext { next ->
                    if (next.data == null ) return@doOnNext

                    Log.d(tag,"remote.cache() local store update cars: "+next.data)
                    //Store shop
                    if (next.data.shop != null){
                        localShopStorage.removeById(next.data.shopId)
                        localShopStorage.storeDealership(next.data.shop)
                    }
                    val localCar = localCarStorage.getCar(next.data!!.id)
                    if (localCar != null && localCar.isCurrentCar) {
                        next.data.isCurrentCar = true
                    }
                    localCarStorage.deleteAndStoreCar(next.data)
                }
    }
}
