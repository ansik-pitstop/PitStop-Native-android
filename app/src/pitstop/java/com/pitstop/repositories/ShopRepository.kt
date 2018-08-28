package com.pitstop.repositories

import android.util.Log

import com.pitstop.BuildConfig
import com.pitstop.database.LocalShopStorage
import com.pitstop.models.Dealership
import com.pitstop.models.DebugMessage
import com.pitstop.network.RequestCallback
import com.pitstop.network.RequestError
import com.pitstop.utils.Logger
import com.pitstop.utils.NetworkHelper

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Created by Matthew on 2017-06-21.
 */

class ShopRepository(private val localShopStorage: LocalShopStorage, private val networkHelper: NetworkHelper) : Repository {

    private val TAG = javaClass.simpleName

    private val END_POINT_SHOP_PITSTOP = "shop?shopType=partner"
    private val END_POINT_SHOP_ALL = "shop?shopType=all"
    private val END_POINT_SHOP = "shop"
    private var removeLocalShop: Boolean = false

    fun getAllShops(callback: Repository.Callback<List<Dealership>>) {

        val localDealerships = localShopStorage.getAllDealerships()
        if (localDealerships.isNotEmpty()) {
            callback.onSuccess(localDealerships)
            return
        }

        networkHelper.get(END_POINT_SHOP_ALL) { response, requestError ->
            if (response != null) {
                try {
                    val dealerships = Dealership.createDealershipList(response)
                    localShopStorage.removeAllDealerships()
                    localShopStorage.storeDealerships(dealerships)
                    callback.onSuccess(dealerships)
                } catch (e: JSONException) {
                    Logger.getInstance()!!.logException(TAG, e, DebugMessage.TYPE_REPO)
                    callback.onError(RequestError.getUnknownError())
                }

            } else {
                callback.onError(requestError)
            }

        }
    }

    fun getPitstopShops(callback: Repository.Callback<List<Dealership>>) {
        networkHelper.get(END_POINT_SHOP_PITSTOP) { response, requestError ->
            if (response != null) {
                try {
                    val dealerships = Dealership.createDealershipList(response)
                    for (d in dealerships) {
                        localShopStorage.removeById(d.id)
                        localShopStorage.storeDealership(d)
                    }
                    callback.onSuccess(dealerships)
                } catch (e: JSONException) {
                    Logger.getInstance()!!.logException(TAG, e, DebugMessage.TYPE_REPO)
                    callback.onError(RequestError.getUnknownError())
                }

            } else {
                callback.onError(requestError)
            }

        }
    }

    fun insert(dealership: Dealership, userId: Int, callback: Repository.Callback<Any>) {
        removeLocalShop = false

        val body = JSONObject()
        try {
            body.put("name", dealership.name)
            body.put("email", dealership.email)
            body.put("phone", dealership.phone)
            body.put("address", dealership.address)
            body.put("googlePlacesId", "")// to be added
        } catch (e: JSONException) {
            Logger.getInstance()!!.logException(TAG, e, DebugMessage.TYPE_REPO)

        }

        networkHelper.post(END_POINT_SHOP, getInsertShopRequestCallback(callback, userId, dealership), body)
    }

    private fun getInsertShopRequestCallback(callback: Repository.Callback<Any>, userId: Int, dealership: Dealership): RequestCallback {
        //Create corresponding request callback

        return RequestCallback { response, requestError ->
            try {
                if (requestError == null) {
                    val shopResponse = JSONObject(response)
                    dealership.id = shopResponse.getInt("id")
                    networkHelper.getUserSettingsById(userId) { response, requestError ->
                        try {
                            if (requestError == null) {
                                val responseJson = JSONObject(response)
                                val userJson = responseJson.getJSONObject("user")
                                var customShops = JSONArray()
                                val userSettingsDealer = JSONObject()
                                userSettingsDealer.put("id", dealership.id)
                                userSettingsDealer.put("name", dealership.name)
                                userSettingsDealer.put("email", dealership.email)
                                userSettingsDealer.put("phone_number", dealership.phone)
                                userSettingsDealer.put("address", dealership.address)
                                if (userJson.has("customShops")) {
                                    val shopsToSend = JSONArray()
                                    customShops = userJson.getJSONArray("customShops")
                                    for (i in 0 until customShops.length()) {
                                        val j = customShops.getJSONObject(i)
                                        if (j.getInt("id") != userSettingsDealer.getInt("id")) {
                                            shopsToSend.put(j)
                                        } else {
                                            removeLocalShop = true
                                        }
                                    }
                                    shopsToSend.put(userSettingsDealer)
                                    userJson.remove("customShops")
                                    userJson.put("customShops", shopsToSend)
                                } else {
                                    customShops.put(userSettingsDealer)
                                    userJson.put("customShops", customShops)
                                }
                                val userSettings = JSONObject()
                                userSettings.put("settings", userJson)
                                networkHelper.put("user/$userId/settings", { response, requestError ->
                                    if (response != null) {
                                        if (removeLocalShop) {
                                            localShopStorage.removeById(dealership.id)
                                        }
                                        localShopStorage.storeCustom(dealership)
                                        callback.onSuccess(response)
                                    } else {
                                        callback.onError(requestError)
                                    }
                                }, userSettings)
                            } else {
                                callback.onError(requestError)
                            }
                        } catch (e: JSONException) {
                            Logger.getInstance()!!.logException(TAG, e, DebugMessage.TYPE_REPO)
                            callback.onError(RequestError.getUnknownError())
                        }
                    }
                } else {
                    callback.onError(requestError)
                }
            } catch (e: JSONException) {
                Logger.getInstance()!!.logException(TAG, e, DebugMessage.TYPE_REPO)
                callback.onError(RequestError.getUnknownError())
            }
        }
    }

    fun update(dealership: Dealership, userId: Int, callback: Repository.Callback<Any>) {
        networkHelper.getUserSettingsById(userId, getUpdateShopRequestCallback(dealership, userId, callback))
    }

    private fun getUpdateShopRequestCallback(dealership: Dealership, userId: Int, callback: Repository.Callback<Any>): RequestCallback {
        return RequestCallback{ response, requestError ->
            if (response != null) {
                try {
                    val responseJson = JSONObject(response)
                    val userJson = responseJson.getJSONObject("user")
                    val customShops = responseJson.getJSONObject("user").getJSONArray("customShops")
                    val shopsToSend = JSONArray()
                    val userSettingsDealer = JSONObject()
                    userSettingsDealer.put("id", dealership.id)
                    userSettingsDealer.put("name", dealership.name)
                    userSettingsDealer.put("email", dealership.email)
                    userSettingsDealer.put("phone_number", dealership.phone)
                    userSettingsDealer.put("address", dealership.address)
                    for (i in 0 until customShops.length()) {
                        val shop = customShops.getJSONObject(i)
                        if (shop.getInt("id") != dealership.id) {
                            shopsToSend.put(shop)
                        }
                    }
                    shopsToSend.put(userSettingsDealer)
                    userJson.remove("customShops")
                    userJson.put("customShops", shopsToSend)
                    val userSettings = JSONObject()
                    userSettings.put("settings", userJson)
                    networkHelper.put("user/$userId/settings", { response, requestError ->
                        if (response != null) {
                            callback.onSuccess(response)
                            localShopStorage.removeById(dealership.id)
                            localShopStorage.storeCustom(dealership)
                        } else {
                            callback.onError(requestError)
                        }
                    }, userSettings)
                } catch (e: JSONException) {
                    Logger.getInstance()!!.logException(TAG, e, DebugMessage.TYPE_REPO)
                    callback.onError(RequestError.getUnknownError())
                }

            } else {
                callback.onError(requestError)
            }
        }

    }


    fun delete(shopId: Int, userId: Int, callback: Repository.Callback<Any>) {
        networkHelper.getUserSettingsById(userId, getDeleteShopRequestCallback(callback, userId, shopId))
    }

    private fun getDeleteShopRequestCallback(callback: Repository.Callback<Any>, userId: Int, shopId: Int): RequestCallback {
        return RequestCallback{ response, requestError ->
            if (response != null) {
                try {
                    val responseJson = JSONObject(response)
                    val userJson = responseJson.getJSONObject("user")
                    val customShops = responseJson.getJSONObject("user").getJSONArray("customShops")
                    val shopsToSend = JSONArray()
                    for (i in 0 until customShops.length()) {
                        val shop = customShops.getJSONObject(i)
                        if (shop.getInt("id") != shopId) {
                            shopsToSend.put(shop)
                        }
                    }
                    userJson.remove("customShops")
                    val userSettings = JSONObject()
                    userJson.put("customShops", shopsToSend)
                    userSettings.put("settings", userJson)
                    networkHelper.put("user/$userId/settings", { response, requestError -> callback.onSuccess(response) }, userSettings)
                } catch (e: JSONException) {
                    Logger.getInstance()!!.logException(TAG, e, DebugMessage.TYPE_REPO)
                    callback.onError(RequestError.getUnknownError())
                }

            } else {
                callback.onError(requestError)
            }
        }
    }


    operator fun get(dealerId: Int, callback: Repository.Callback<Dealership>) {
        var dealerId = dealerId
        Log.d(TAG, "get() $END_POINT_SHOP/$dealerId")

        if (dealerId == 0 && (BuildConfig.DEBUG || BuildConfig.BUILD_TYPE == BuildConfig.BUILD_TYPE_BETA)) {
            dealerId = 1
        } else if (dealerId == 0 && BuildConfig.BUILD_TYPE == BuildConfig.BUILD_TYPE_RELEASE) {
            dealerId = 19
        }
        val localDealership = localShopStorage.getDealership(dealerId)
        if (localDealership != null) {
            callback.onSuccess(localDealership)
            return
        } else
            networkHelper.get("$END_POINT_SHOP/$dealerId", getGetShopRequestCallback(callback))

    }

    private fun getGetShopRequestCallback(callback: Repository.Callback<Dealership>): RequestCallback {
        return RequestCallback{ response, requestError ->
            if (response != null) {
                //try{
                Log.d(TAG, "get shop response: " + response!!)
                val dealership = Dealership.jsonToDealershipObject(response)
                if (dealership == null) {
                    callback.onError(RequestError.getUnknownError())
                } else {
                    callback.onSuccess(dealership)
                }
            } else {
                callback.onError(requestError)
            }
        }
    }
}
