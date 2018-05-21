package com.pitstop.repositories

import android.util.Log
import com.google.gson.Gson
import com.jakewharton.retrofit2.adapter.rxjava2.HttpException
import com.pitstop.database.LocalSensorDataStorage
import com.pitstop.models.DebugMessage
import com.pitstop.models.sensor_data.SensorData
import com.pitstop.retrofit.PitstopSensorDataApi
import com.pitstop.utils.Logger
import com.pitstop.utils.SensorDataUtils
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers

/**
 * Created by Karol Zdebel on 4/18/2018.
 */
class SensorDataRepository(private val local: LocalSensorDataStorage
                           , private val remote: PitstopSensorDataApi
                           , private val connectionObservable: Observable<Boolean>): Repository {

    private val TAG = SensorDataRepository::class.java.simpleName
    private val CHUNK_SIZE = 10
    private val gson = Gson()
    private var dumping: Boolean = false

    fun dumpDataOnConnectedToNetwork(){
        //Begins dumping data on connected to internet
        if (dumping) return
        dumping = true
        connectionObservable.subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe({next ->
                    Log.d(TAG,"connectionObservable onNext(): $next")
                    if (next){
                        dumpData().subscribeOn(Schedulers.io())
                                .observeOn(Schedulers.io())
                                .subscribe({next ->
                                    Log.d(TAG,"dumpData data response: "+next)
                                })
                    }
                }, {error ->
                    Log.d(TAG,"connectionObservable onError() err: $error")
                })
    }

    fun store(sensorData: Collection<SensorData>){
        Log.d(TAG,"store()")
        sensorData.forEach { local.store(it) }
    }

    fun store(sensorData: SensorData){
        Log.d(TAG,"store()")
        local.store(sensorData)
    }

    fun dumpData(): Observable<Int> {
        Log.d(TAG,"dumpData()")
        val observableList = arrayListOf<Observable<Int>>()

        local.getAll().chunked(CHUNK_SIZE).forEach {
            val body = gson.toJsonTree(SensorDataUtils.sensorDataListToDataPointList(it))
            Log.d(TAG,"body: $body")
            val remoteObservable = remote.store(body)
            remoteObservable
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .subscribe({next ->
                        Log.d(TAG,"Stored chunk, response: $next")
                        local.delete(it) }
                    ,{err ->
                        var message: String
                        if (err is HttpException){
                            message = err.response().message().toString()
                            if (err.code().toString().isNotEmpty() && err.code().toString()[0] == '4'){
                                //Remove data that failed to send due to server rejection
                                local.delete(it)
                            }
                        }else{
                            message = err.toString()
                        }
                        Logger.getInstance().logE(TAG, "Error storing chunk = $message, cause = ${err.cause}"
                            , DebugMessage.TYPE_REPO)
                    })

            observableList.add(remoteObservable.cache()
                    .map { CHUNK_SIZE })
        }

        return Observable.combineLatestDelayError(observableList,{ list ->
            Log.d(TAG,"observable.combineLatest()")
            list.sumBy { (it as Int) }
        })
    }

    fun storeThenDump(sensorData: Collection<SensorData>): Observable<Int>{
        store(sensorData)
        return dumpData()
    }

    fun storeThenDump(sensorData: SensorData): Observable<Int>{
        store(sensorData)
        return dumpData()
    }

    fun deleteAll(){
        local.deleteAll()
    }
}