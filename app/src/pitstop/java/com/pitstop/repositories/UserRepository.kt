package com.pitstop.repositories

import android.util.Log
import com.google.gson.JsonIOException
import com.google.gson.JsonObject
import com.parse.ParseInstallation
import com.pitstop.database.LocalUserStorage
import com.pitstop.models.DebugMessage
import com.pitstop.models.Settings
import com.pitstop.models.User
import com.pitstop.network.RequestCallback
import com.pitstop.network.RequestError
import com.pitstop.retrofit.*
import com.pitstop.utils.Logger
import com.pitstop.utils.NetworkHelper
import io.reactivex.Observable
import org.json.JSONException
import org.json.JSONObject

/**
 * User repository, use this class to modify, retrieve, and delete user data.
 * Updates data both remotely and locally.
 *
 * Created by Karol Zdebel on 5/29/2017.
 */

class UserRepository(private val localUserStorage: LocalUserStorage
                     , private val pitstopUserApi: PitstopUserApi
                     , private val pitstopAuthApi: PitstopAuthApi
                     , private val networkHelper: NetworkHelper) : Repository {

    private val TAG = javaClass.simpleName

    private val END_POINT_SETTINGS = "settings/?userId="
    private val END_POINT_USER = "user/"

    fun insert(user: User, isSocial: Boolean): Observable<User> {
        Log.d(TAG, "insert() model: $user")
        val json = JsonObject()
        try {
            json.addProperty("firstName", user.firstName)
            json.addProperty("lastName", user.lastName)
            json.addProperty("email", user.email)
            json.addProperty("username", user.email)
            json.addProperty("activated", true)
            if (!isSocial){
                json.addProperty("password", user.password)
                json.addProperty("phone", user.phone)
            }
            json.addProperty("isSocial", isSocial)
            json.addProperty("installationId"
                        , ParseInstallation.getCurrentInstallation().installationId)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        Log.d(TAG,"insert user body: $json")
        return pitstopAuthApi.signUp(json)
                .doOnNext({
                    Log.d(TAG,"next: $it")
                    localUserStorage.deleteAllUsers()
                    it.settings.userId = it.id
                    localUserStorage.storeUserData(it)
                })
    }

    fun loginFacebook(accessToken: String): Observable<LoginResponse>{
        Log.d(TAG,"loginFacebook() token: $accessToken")

        val json = JsonObject()
        try {
            json.addProperty("accessToken", accessToken)
            json.addProperty("provider", "facebook")
            json.addProperty("installationId"
                    , ParseInstallation.getCurrentInstallation().installationId)
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        Log.d(TAG,"body: $json")

        return pitstopAuthApi.loginSocial(json)
                .doOnNext({
                    localUserStorage.deleteAllUsers()
                    it.user.settings.userId = it.user.id
                    localUserStorage.storeUserData(it.user)
                })
    }

    fun setUserActive(userId: Int): Observable<UserActivationResponse>{
        Log.d(TAG,"setUserActive()")
        val jsonObject = JsonObject()
        jsonObject.addProperty("userId",userId)
        jsonObject.addProperty("activated",true)
        return pitstopUserApi.putUser(jsonObject).map{
            UserActivationResponse(it.get("userId").asInt,it.get("activated").asBoolean)
        }
    }

    fun changeUserPassword(userId: Int, oldPassword: String, newPassword: String): Observable<ChangePasswordResponse>{
        Log.d(TAG,"changeUserPassword()")
        val jsonObject = JsonObject()
        jsonObject.addProperty("oldPass",oldPassword)
        jsonObject.addProperty("newPass",newPassword)
        return pitstopUserApi.changePassword(userId,jsonObject)
    }

    fun resetPassword(email: String): Observable<ChangePasswordResponse>{
        Log.d(TAG,"resetPassword() email: $email")
        val jsonObject = JsonObject()
        jsonObject.addProperty("email",email)
        return pitstopAuthApi.resetPassword(jsonObject)
    }

    fun login(username: String, password: String): Observable<LoginResponse>{
        Log.d(TAG,"login()")

        val json = JsonObject()
        try {
            json.addProperty("username", username)
            json.addProperty("password", password)
            json.addProperty("installationId"
                    ,ParseInstallation.getCurrentInstallation().installationId)
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        return pitstopAuthApi.login(json)
                .doOnNext({
                    Log.d(TAG,"LoginResponse: $it")
                    localUserStorage.deleteAllUsers()
                    it.user.settings.userId = it.user.id
                    localUserStorage.storeUserData(it.user)
                })
    }

    fun update(model: User, callback: Repository.Callback<Any>) {
        Log.d(TAG, "update() user: $model")
        localUserStorage.updateUser(model)
        updateUser(model.id, model.firstName, model.lastName, model.phone ?: "", getUserUpdateRequestCallback(callback, model))
    }

    private fun getUserUpdateRequestCallback(callback: Repository.Callback<Any>, user: User): RequestCallback {
        //Create corresponding request callback

        return RequestCallback { response, requestError ->
            try {
                if (requestError == null && localUserStorage.user != null) {
                    val newUser = localUserStorage.user
                    user.firstName = user.firstName
                    user.lastName = user.lastName
                    user.phone = user.phone
                    localUserStorage.updateUser(newUser)
                    callback.onSuccess(null)
                } else {
                    callback.onError(requestError)
                }
            } catch (e: JsonIOException) {
                Logger.getInstance()!!.logException(TAG, e, DebugMessage.TYPE_REPO)
                callback.onError(requestError)
            }
        }
    }

    fun getCurrentUser(callback: Repository.Callback<User>) {
        Log.d(TAG, "getCurrentUser() user: ${localUserStorage.user}")
        if (localUserStorage.user == null) {
            callback.onError(RequestError.getUnknownError())
            return
        } else {
            callback.onSuccess(localUserStorage.user)
            return
        }
    }

    fun getRemoteCurrentUser(callback: Repository.Callback<User>) {
        Log.d(TAG, "getRemoteCurrentUser()")
        if (localUserStorage.user == null) {
            callback.onError(RequestError.getUnknownError())
        } else
            networkHelper.get(END_POINT_USER + localUserStorage.user!!.id, getUserGetRequestCallback(callback))
    }

    private fun getUserGetRequestCallback(callback: Repository.Callback<User>): RequestCallback {
        //Create corresponding request callback

        return RequestCallback { response, requestError ->
            try {
                if (requestError == null) {
                    Log.d("userresponse", response)
                    callback.onSuccess(User.jsonToUserObject(response))
                } else {
                    callback.onError(requestError)
                }
            } catch (e: JsonIOException) {
                Logger.getInstance()!!.logException(TAG, e, DebugMessage.TYPE_REPO)
                callback.onError(RequestError.getUnknownError())
            }
        }
    }

    fun setUserCar(userId: Int, carId: Int, callback: Repository.Callback<Any>) {
        getUserSettings(userId, RequestCallback{ response, requestError ->
            if (requestError == null) {
                try {
                    val options = JSONObject(response).getJSONObject("user")
                    options.put("mainCar", carId)
                    val putOptions = JSONObject()
                    putOptions.put("settings", options)

                    networkHelper.put("user/$userId/settings", getUserSetCarRequestCallback(callback, carId), putOptions)
                } catch (e: JSONException) {
                    Logger.getInstance()!!.logException(TAG, e, DebugMessage.TYPE_REPO)
                    callback.onError(RequestError.getUnknownError())
                }

            } else {
                callback.onError(requestError)
            }
        })
    }

    private fun getUserSetCarRequestCallback(callback: Repository.Callback<Any>, carId: Int): RequestCallback {
        //Create corresponding request callback
        return RequestCallback{ response, requestError ->
            Log.d(TAG, "set user settings response: $response, requestError: $requestError")
            try {
                if (requestError == null && localUserStorage.user != null) {
                    val user = localUserStorage.user
                    val settingsNew = user!!.settings
                    settingsNew.carId = carId
                    user.settings = settingsNew
                    localUserStorage.updateUser(user)
                    Log.d(TAG, "setUserCar() updating user to: " + localUserStorage.user!!.settings.carId)
                    callback.onSuccess(response)
                } else {
                    callback.onError(requestError)
                }
            } catch (e: JsonIOException) {
                Logger.getInstance()!!.logException(TAG, e, DebugMessage.TYPE_REPO)
                callback.onError(requestError)
            }
        }
    }

    fun setFirstCarAdded(added: Boolean, callback: Repository.Callback<Any>) {

        Log.d(TAG, "setFirstCarAdded() added: $added")
        val userId = localUserStorage.user!!.id

        getUserSettings(userId, RequestCallback{ response, requestError ->
            if (requestError == null) {
                try {
                    //Get settings and add boolean
                    val settings = JSONObject(response).getJSONObject("user")
                    settings.put("isFirstCarAdded", added)

                    val putSettings = JSONObject()
                    putSettings.put("settings", settings)

                    val requestCallback = getSetFirstCarAddedCallback(callback, added)

                    networkHelper.put("user/$userId/settings", requestCallback, putSettings)
                } catch (e: JSONException) {
                    Logger.getInstance()!!.logException(TAG, e, DebugMessage.TYPE_REPO)
                    callback.onError(requestError)
                }

            } else {
                callback.onError(requestError)
            }
        })
    }


    fun setAlarmsEnabled(alarmsEnabled: Boolean, callback: Repository.Callback<Any>) {

        val userId = localUserStorage.user?.id
        if (userId == null) {
            callback.onError(RequestError.getUnknownError())
            return
        }

        getUserSettings(userId, RequestCallback { response, requestError ->
            if (requestError == null) {
                try {
                    //Get settings and add boolean
                    val settings = JSONObject(response).getJSONObject("user")
                    settings.put("alarmsEnabled", alarmsEnabled)

                    val putSettings = JSONObject()
                    putSettings.put("settings", settings)

                    val requestCallback = getSetAlarmsEnabledCallback(callback, alarmsEnabled)

                    networkHelper.put("user/$userId/settings", requestCallback, putSettings)

                    Log.d("alarms:", settings.toString())
                } catch (e: JSONException) {
                    Logger.getInstance()!!.logException(TAG, e, DebugMessage.TYPE_REPO)
                    callback.onError(requestError)
                }

            } else {
                callback.onError(requestError)
            }
        })
    }

    private fun getSetAlarmsEnabledCallback(callback: Repository.Callback<Any>, enabled: Boolean): RequestCallback {
        return RequestCallback { response, requestError ->
            try {
                if (requestError == null && localUserStorage.user != null) {
                    val user = localUserStorage.user
                    val settingsNew = user!!.settings
                    settingsNew.isAlarmsEnabled = enabled
                    user.settings = settingsNew
                    localUserStorage.updateUser(user)
                    callback.onSuccess(response)
                } else {
                    callback.onError(requestError)
                }
            } catch (e: JsonIOException) {
                Logger.getInstance()!!.logException(TAG, e, DebugMessage.TYPE_REPO)
                callback.onError(requestError)
            }
        }
    }


    private fun getSetFirstCarAddedCallback(callback: Repository.Callback<Any>, added: Boolean): RequestCallback {
        //Create corresponding request callback
        return RequestCallback{ response, requestError ->
            try {
                if (requestError == null && localUserStorage.user != null) {
                    val user = localUserStorage.user
                    val settingsNew = user!!.settings
                    settingsNew.isFirstCarAdded = added
                    user.settings = settingsNew
                    localUserStorage.updateUser(user)
                    callback.onSuccess(response)
                } else {
                    callback.onError(requestError)
                }
            } catch (e: JsonIOException) {
                Logger.getInstance()!!.logException(TAG, e, DebugMessage.TYPE_REPO)
                callback.onError(requestError)
            }
        }
    }


    fun getCurrentUserSettings(callback: Repository.Callback<Settings>) {
        Log.d(TAG, "getCurrentUserSettings()")
        if (localUserStorage.user == null) {
            callback.onError(RequestError.getUnknownError())
            return
        } else if (localUserStorage.user!!.settings != null) {
            Log.d(TAG, "Returning local settings! settings: " + localUserStorage.user!!.settings)
            callback.onSuccess(localUserStorage.user!!.settings)
            return
        }
        Log.d(TAG, "Fetching settings remotely!()")
        val userId = localUserStorage.user!!.id

        getUserSettings(userId, RequestCallback{ response, requestError ->
            if (requestError != null || localUserStorage.user == null) {
                callback.onError(requestError)
                return@RequestCallback
            }
            try {
                val settings = JSONObject(response)
                var carId = -1
                var firstCarAdded = true //if not present, default is true
                var alarmsEnabled = false // if not present, default is false

                if (settings.getJSONObject("user").has("alarmsEnabled")) {
                    alarmsEnabled = settings.getJSONObject("user").getBoolean("alarmsEnabled")
                }
                if (settings.getJSONObject("user").has("isFirstCarAdded")) {
                    firstCarAdded = settings.getJSONObject("user").getBoolean("isFirstCarAdded")
                }
                if (settings.getJSONObject("user").has("mainCar")) {
                    carId = settings.getJSONObject("user").getInt("mainCar")
                }
                val user = localUserStorage.user

                user!!.settings = Settings(user.id, carId, firstCarAdded, alarmsEnabled)
                localUserStorage.updateUser(user)
                callback.onSuccess(user.settings)
            } catch (e: JSONException) {
                Logger.getInstance()!!.logException(TAG, e, DebugMessage.TYPE_REPO)
                callback.onError(RequestError.getUnknownError())
            }
        })
    }

    private fun getUserSettings(userId: Int, callback: RequestCallback) {
        networkHelper.get(END_POINT_SETTINGS + userId, callback)
    }

    private fun updateUser(userId: Int, firstName: String, lastName: String, phoneNumber: String, callback: RequestCallback) {
        try {
            val json = JSONObject()
            json.put("userId", userId)
            json.put("firstName", firstName)
            json.put("lastName", lastName)
            json.put("phone", phoneNumber)
            networkHelper.put(END_POINT_USER, callback, json)
        } catch (e: JSONException) {
            Logger.getInstance()!!.logException(TAG, e, DebugMessage.TYPE_REPO)
            callback.done(null, RequestError.getUnknownError())
        }

    }
}
