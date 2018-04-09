package com.pitstop.interactors.get

import android.location.Location
import android.os.Handler
import android.util.Log
import com.jakewharton.retrofit2.adapter.rxjava2.HttpException
import com.pitstop.models.DebugMessage
import com.pitstop.models.snapToRoad.SnappedPoint
import com.pitstop.network.RequestError
import com.pitstop.repositories.SnapToRoadRepository
import com.pitstop.utils.Logger
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

/**
 * Created by David C. on 16/3/18.
 */
class GetSnapToRoadUseCaseImpl(private val snapToRoadRepository: SnapToRoadRepository,
                               private val useCaseHandler: Handler,
                               private val mainHandler: Handler) : GetSnapToRoadUseCase {

    private val tag = javaClass.simpleName
    private lateinit var callback: GetSnapToRoadUseCase.Callback
    private lateinit var polylineList: List<Location>

    override fun execute(polylineList: List<Location>, callback: GetSnapToRoadUseCase.Callback) {
        Logger.getInstance()!!.logI(tag, "Use case execution started: polyline = $polylineList", DebugMessage.TYPE_USE_CASE)
        this.callback = callback
        this.polylineList = polylineList
        useCaseHandler.post(this)
    }

    override fun run() {

        Log.d(tag, "run()")

        val index = 0
        val overlap = 10
        var nextPartitionIndex = 100
        val listSizeLimit = 100
        var polylinePartition = arrayListOf<Location>()
        var observableList = arrayListOf<Observable<List<SnappedPoint>>>()

        while (index < polylineList.size){
            polylinePartition.add(polylineList[index])

            if (index != 0 && index == nextPartitionIndex){
                polylinePartition = arrayListOf()
                index.minus(overlap)
                nextPartitionIndex = index + listSizeLimit

                //send request
                val latLngString: String = ""
                polylineList.forEach({ loc ->
                    latLngString.plus("${loc.latitude},${loc.longitude}")
                    if (polylineList.last() != loc) latLngString.plus("|")
                })
                observableList.add(snapToRoadRepository.getSnapToRoadFromLocations(latLngString)
                        //Remove overlap points on all paritions other than first
                        .map {
                            return@map if (!observableList.isEmpty())
                                it.data!!.subList(overlap,it.data.lastIndex)
                            else it.data
                        })
            }else{
                index.inc()
            }
        }

        Observable.concat(observableList)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.from(useCaseHandler.looper))
                .subscribe({
                    Log.d(tag, "snapToRoadRepository.onNext() data: " + it)
                    this@GetSnapToRoadUseCaseImpl.onSnapToRoadRetrieved(it)

                }, { error ->
                    if (error is HttpException){
                        val errorBody = error.response().errorBody()?.string()
                        Log.d(tag,"snapToRoadRepository.error: $errorBody")
                    }
                    this@GetSnapToRoadUseCaseImpl.onError(RequestError(error))

                })

    }

    private fun onSnapToRoadRetrieved(snappedPointList: List<SnappedPoint>) {

        Logger.getInstance()!!.logI(tag, "Use case finished result: trips=$snappedPointList", DebugMessage.TYPE_USE_CASE)
        mainHandler.post({ callback.onSnapToRoadRetrieved(snappedPointList) })

    }

    private fun onError(error: RequestError?) {
        if (error != null) {
            Logger.getInstance()!!.logE(tag, "Use case returned error: err=" + error, DebugMessage.TYPE_USE_CASE)
            mainHandler.post({ callback.onError(error) })
        } else {
            Logger.getInstance()!!.logE(tag, "Use case returned error: err="
                    + RequestError.getUnknownError(), DebugMessage.TYPE_USE_CASE)
            mainHandler.post({ callback.onError(RequestError.getUnknownError()) })
        }
    }

}