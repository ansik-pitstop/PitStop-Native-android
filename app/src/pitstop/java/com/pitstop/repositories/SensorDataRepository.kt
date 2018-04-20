package com.pitstop.repositories

import android.util.Log
import com.google.gson.Gson
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

    fun store(sensorData: SensorData){
        Log.d(TAG,"store()")
        local.store(sensorData)
    }

    fun dumpData(): Observable<Int> {
        Log.d(TAG,"dumpData()")
        val observableList = arrayListOf<Observable<Int>>()

        local.getAll().chunked(CHUNK_SIZE).forEach {
            val body = gson.toJsonTree(SensorDataUtils.sensorDataListToDataPointList(it))
            val remoteObservable = remote.store(body)
            remoteObservable
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .subscribe({next ->
                        Log.d(TAG,"Stored chunk, response: $next")
                        local.delete(it) }
                    ,{err ->
                        Logger.getInstance().logE(TAG, "Error storing chunk = $err"
                            , DebugMessage.TYPE_REPO)
                    })

            observableList.add(remoteObservable.cache()
                    .map { CHUNK_SIZE }
                    .onErrorResumeNext { t:Throwable ->
                            Log.d(TAG,"observableList.add().onErrorResumeNext() returning 0")
                            Observable.just(0)
            })
        }

        return Observable.combineLatest(observableList,{ list ->
            Log.d(TAG,"observable.combineLatest()")
            list.sumBy { (it as Int) }
        })
    }

    fun storeThenDump(sensorData: SensorData){
        store(sensorData)
        dumpData()
    }

    fun deleteAll(){
        local.deleteAll()
    }
}