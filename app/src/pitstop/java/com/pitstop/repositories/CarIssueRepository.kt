package com.pitstop.repositories

import android.util.Log
import com.google.gson.JsonIOException
import com.google.gson.JsonObject
import com.pitstop.database.LocalCarIssueStorage
import com.pitstop.models.Appointment
import com.pitstop.models.DebugMessage
import com.pitstop.models.issue.CarIssue
import com.pitstop.models.issue.IssueDetail
import com.pitstop.models.issue.UpcomingIssue
import com.pitstop.network.RequestCallback
import com.pitstop.network.RequestError
import com.pitstop.retrofit.PitstopServiceApi
import com.pitstop.ui.service_request.RequestServiceActivity
import com.pitstop.utils.Logger
import com.pitstop.utils.NetworkHelper
import io.reactivex.Observable
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*


/**
 * CarIssue repository, use this class to modify, retrieve, and delete car issue data.
 * Updates data both remotely and locally.
 *
 * Created by Karol Zdebel on 5/29/2017.
 */

class CarIssueRepository(private val localCarIssueStorage: LocalCarIssueStorage
                         , private val pitstopServiceApi: PitstopServiceApi
                         , private val networkHelper: NetworkHelper) : Repository {

    private val TAG = CarIssueRepository::class.java.simpleName
    private val END_POINT_REQUEST_SERVICE = "utility/serviceRequest"

    fun insertDtc(carId: Int, mileage: Double, rtcTime: Long, dtcCode: String, isPending: Boolean, callback: Repository.Callback<String>) {
        Log.d(TAG, "insertDtc() dtcCode: $dtcCode")
        val body = JSONObject()

        try {

            body.put("carId", carId)
            body.put("issueType", CarIssue.DTC)
            body.put("data",
                    JSONObject().put("mileage", mileage)
                            .put("rtcTime", rtcTime)
                            .put("dtcCode", dtcCode)
                            .put("isPending", isPending))
        } catch (e: JSONException) {
            Logger.getInstance()!!.logException(TAG, e, DebugMessage.TYPE_REPO)
            e.printStackTrace()
        }

        networkHelper.post("issue", { response, requestError ->
            if (requestError == null) {
                callback.onSuccess(dtcCode)
            } else {
                callback.onError(requestError)
                Log.d(TAG, "insertDtc() ERROR: "
                        + requestError.message + ", body: " + body.toString())
            }
        }, body)
    }

    fun deleteLocalCarIssueData(carId: Int){
        localCarIssueStorage.deleteAllCarIssues(carId)
    }

    fun insert(issue: CarIssue, callback: Repository.Callback<Any>) {

        val body = JSONObject()
        val data = JSONArray()

        try {
            if (issue.issueType == CarIssue.TYPE_PRESET) {
                data.put(JSONObject()
                        .put("type", issue.issueType)
                        .put("status", issue.status)
                        .put("id", issue.id))
            } else {
                data.put(JSONObject()
                        .put("type", issue.issueType)
                        .put("item", issue.item)
                        .put("action", issue.action)
                        .put("description", issue.description)
                        .put("priority", issue.priority))
            }
        } catch (e: JSONException) {
            Logger.getInstance()!!.logException(TAG, e, DebugMessage.TYPE_REPO)
            e.printStackTrace()
        }

        Log.d(TAG, "insert() body: " + body.toString())
        networkHelper.post("car/" + issue.carId + "/service", getInsertCarIssueRequestCallback(callback), body)
    }

    fun insert(carId: Int, issues: List<CarIssue>, callback: Repository.Callback<Any>) {
        val body = JSONObject()
        val data = JSONArray()
        try {
            for (issue in issues) {
                if (issue.issueType == CarIssue.TYPE_PRESET) {
                    data.put(JSONObject()
                            .put("type", issue.issueType)
                            .put("status", issue.status)
                            .put("id", issue.id))
                } else {
                    data.put(JSONObject()
                            .put("type", issue.issueType)
                            .put("item", issue.item)
                            .put("action", issue.action)
                            .put("description", issue.description)
                            .put("priority", issue.priority))
                }
            }
            body.put("data", data)
        } catch (e: JSONException) {
            Logger.getInstance()!!.logException(TAG, e, DebugMessage.TYPE_REPO)
            e.printStackTrace()
        }

        Log.d(TAG, "insert() body: " + body.toString())
        networkHelper.post("car/$carId/service", getInsertCarIssuesRequestCallback(callback), body)
    }

    private fun getInsertCarIssuesRequestCallback(callback: Repository.Callback<Any>): RequestCallback {
        return RequestCallback{ response, requestError ->
            if (requestError == null) {
                callback.onSuccess(response)

            } else {
                callback.onError(requestError)
            }
        }
    }

    private fun getInsertCarIssueRequestCallback(callback: Repository.Callback<Any>): RequestCallback {
        //Create corresponding request callback

        return RequestCallback{ response, requestError ->
            try {
                if (requestError == null) {
                    callback.onSuccess(response)
                } else {
                    callback.onError(requestError)
                }
            } catch (e: JsonIOException) {
                Logger.getInstance()!!.logException(TAG, e, DebugMessage.TYPE_REPO)
                callback.onError(RequestError.getUnknownError())
            }
        }
    }

    fun insertCustom(carId: Int, userid: Int, issue: CarIssue, callback: Repository.Callback<CarIssue>) {
        val body = JSONObject()
        try {
            body.put("carId", carId)
            body.put("issueType", "customUser")
            val data = JSONObject()
            data.put("item", issue.item)
            data.put("action", issue.action)
            data.put("priority", issue.priority)
            data.put("description", issue.description)
            data.put("userId", userid)
            body.put("data", data)

        } catch (e: JSONException) {
            Logger.getInstance()!!.logException(TAG, e, DebugMessage.TYPE_REPO)
            callback.onError(RequestError.getUnknownError())
        }

        networkHelper.post("issue", getInsertCustomRequestCallback(callback, carId), body)

    }

    fun getInsertCustomRequestCallback(callback: Repository.Callback<CarIssue>, carId: Int): RequestCallback {
        return RequestCallback{ response, requestError ->
            if (requestError == null) {
                val issue = CarIssue()
                try {
                    val responseJson = JSONObject(response)
                    issue.carId = carId
                    issue.id = responseJson.getInt("id")
                    val item = responseJson.getString("item")
                    val action = responseJson.getString("action")
                    val description = responseJson.getString("description")
                    issue.issueDetail = IssueDetail(item,action,description)
                    issue.issueType = CarIssue.SERVICE_USER
                    callback.onSuccess(issue)
                } catch (e: JSONException) {
                    Logger.getInstance()!!.logException(TAG, e, DebugMessage.TYPE_REPO)
                    callback.onSuccess(CarIssue())
                }

            } else {
                callback.onError(requestError)
            }
        }
    }

    fun markDone(issue: CarIssue): Observable<CarIssue> {
        val body = JsonObject()
        try {
            body.addProperty("carId", issue.carId)
            body.addProperty("issueId", issue.id)
            body.addProperty("status", issue.status)

            if (issue.status == CarIssue.ISSUE_DONE) {
                body.addProperty("daysAgo", issue.daysAgo)
                body.addProperty("mileage", issue.doneMileage)
            }

        } catch (e: Exception) {
            Logger.getInstance()!!.logException(TAG, e, DebugMessage.TYPE_REPO)
            e.printStackTrace()
        }

       return pitstopServiceApi.markDone(body)
               .doOnNext({
                    Log.d(TAG,"got issue from server: $it")
                   localCarIssueStorage.markIssueDone(issue.id, it.doneAt)
               })

    }

    fun getUpcomingCarIssues(carId: Int, databaseType: Repository.DATABASE_TYPE): Observable<RepositoryResponse<List<UpcomingIssue>>>{

        val remote = pitstopServiceApi.getUpcomingServices(carId)
                .map { RepositoryResponse(it.results[0].issues,false) }
                .doOnNext({
                    Log.d(TAG,"Got data:" +it.data)
                    val storedRows = localCarIssueStorage.replaceUpcomingIssues(carId,it.data!!)
                    Log.d(TAG, "Stored $storedRows upcoming issues locally.")
                })
        val local = Observable.just(localCarIssueStorage.getAllUpcomingCarIssues())
                .map { RepositoryResponse(it,true) }

        return when (databaseType){
            Repository.DATABASE_TYPE.LOCAL -> local
            Repository.DATABASE_TYPE.REMOTE -> remote
            Repository.DATABASE_TYPE.BOTH -> {
                val list: MutableList<Observable<RepositoryResponse<List<UpcomingIssue>>>> = mutableListOf()
                list.add(local)
                list.add(remote)
                Observable.concatDelayError(list)
            }
        }
    }

    fun getCurrentCarIssues(carId: Int, databaseType: Repository.DATABASE_TYPE): Observable<RepositoryResponse<List<CarIssue>>> {
        val remote = pitstopServiceApi.getCurrentServices(carId)
                .map{ RepositoryResponse(it.results,false)}
                .doOnNext({
                    Log.d(TAG,"got data: "+it.data)
                    val storedRows = localCarIssueStorage.replaceCurrentIssues(carId,it.data!!)
                    Log.d(TAG, "Stored $storedRows current issues locally.")
                })
        val local = Observable.just(localCarIssueStorage.getAllCurrentCarIssues())
                .map {
                    RepositoryResponse(it,true)
                }

        return when (databaseType){
            Repository.DATABASE_TYPE.LOCAL -> local
            Repository.DATABASE_TYPE.REMOTE -> remote
            Repository.DATABASE_TYPE.BOTH -> {
                val list: MutableList<Observable<RepositoryResponse<List<CarIssue>>>> = mutableListOf()
                list.add(local)
                list.add(remote)
                Observable.concatDelayError(list)
            }
        }
    }

    fun getDoneCarIssues(carId: Int, databaseType: Repository.DATABASE_TYPE): Observable<RepositoryResponse<List<CarIssue>>> {
        val remote = pitstopServiceApi.getDoneServices(carId)
                .map{ RepositoryResponse(it.results,false) }
                .doOnNext({
                    val storedDoneIssues = localCarIssueStorage.replaceDoneIssues(carId,it.data!!)
                    Log.d(TAG, "Stored $storedDoneIssues done issues locally.")
                })
        val local = Observable.just(localCarIssueStorage.getAllDoneCarIssues())
                .map {
                    RepositoryResponse(it,true)
                }

        return when (databaseType){
            Repository.DATABASE_TYPE.LOCAL -> local
            Repository.DATABASE_TYPE.REMOTE -> remote
            Repository.DATABASE_TYPE.BOTH -> {
                val list: MutableList<Observable<RepositoryResponse<List<CarIssue>>>> = mutableListOf()
                list.add(local)
                list.add(remote)
                Observable.concatDelayError(list)
            }
        }
    }

    fun requestService(userId: Int, carId: Int, appointment: Appointment, callback: Repository.Callback<Any>) {
        Log.d(TAG, "requestService() userId $userId, carId: $carId, appointment: $appointment")
        val body = JSONObject()
        val options = JSONObject()
        try {
            body.put("userId", userId)
            body.put("carId", carId)
            body.put("shopId", appointment.shopId)
            body.put("comments", appointment.comments)
            options.put("state", appointment.state)
            val stringDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CANADA)
                    .format(appointment.date)
            options.put("appointmentDate", stringDate)
            body.put("options", options)
        } catch (e: JSONException) {
            Logger.getInstance()!!.logException(TAG, e, DebugMessage.TYPE_REPO)
            e.printStackTrace()
            callback.onError(RequestError.getUnknownError())
        }

        networkHelper.post(END_POINT_REQUEST_SERVICE, getRequestServiceCallback(callback), body)

        // If state is tentative, we put salesPerson to another endpoint
        if (appointment.state == RequestServiceActivity.activityResult.STATE_TENTATIVE) {
            val updateSalesman = JSONObject()
            try {
                updateSalesman.put("carId", carId)
                updateSalesman.put("salesperson", appointment.comments)
            } catch (e: JSONException) {
                Logger.getInstance()!!.logException(TAG, e, DebugMessage.TYPE_REPO)
                e.printStackTrace()
                callback.onError(RequestError.getUnknownError())
            }

            networkHelper.put("car", { response, requestError -> }, updateSalesman)
        }
    }

    private fun getRequestServiceCallback(callback: Repository.Callback<Any>): RequestCallback {
        return RequestCallback{ response, requestError ->
            if (requestError == null) {
                callback.onSuccess(response)
            } else {
                callback.onError(requestError)
            }
        }
    }

}
