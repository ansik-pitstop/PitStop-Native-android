package com.pitstop.interactors.get

import android.os.Handler
import android.util.Log
import com.pitstop.models.DebugMessage
import com.pitstop.models.snapToRoad.SnappedPoint
import com.pitstop.network.RequestError
import com.pitstop.repositories.SnapToRoadRepository
import com.pitstop.utils.Logger
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

/**
 * Created by David C. on 16/3/18.
 */
class GetSnapToRoadUseCaseImpl(private val snapToRoadRepository: SnapToRoadRepository,
                               private val useCaseHandler: Handler,
                               private val mainHandler: Handler) : GetSnapToRoadUseCase {

    private val tag = javaClass.simpleName
    private var callback: GetSnapToRoadUseCase.Callback? = null
    private var listLatLng: String? = null
    private var interpolate: String? = null
    private var apiKey: String? = null

    override fun execute(listLatLng: String, interpolate: String, apiKey: String, callback: GetSnapToRoadUseCase.Callback) {
        Logger.getInstance()!!.logI(tag, "Use case execution started listLatLng: $listLatLng", DebugMessage.TYPE_USE_CASE)
        this.callback = callback
        this.listLatLng = listLatLng
        this.interpolate = interpolate
        this.apiKey = apiKey;
        useCaseHandler.post(this)
    }

    override fun run() {

        Log.d(tag, "run()")

        snapToRoadRepository.getSnapToRoadFromLocations(listLatLng!!, interpolate!!, apiKey!!)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.from(useCaseHandler.looper))
                .subscribe({next ->

                    Log.d(tag, "tripRepository.onNext() data: " + next)
                    this@GetSnapToRoadUseCaseImpl.onSnapToRoadRetrieved(next.data.orEmpty())

                }, { error ->
                    Log.d(tag,"snapToRoadRepository.error: $error")
                    this@GetSnapToRoadUseCaseImpl.onError(RequestError(error))

                })

    }

    private fun onSnapToRoadRetrieved(snappedPointList: List<SnappedPoint>) {

        Logger.getInstance()!!.logI(tag, "Use case finished result: trips=$snappedPointList", DebugMessage.TYPE_USE_CASE)
        mainHandler.post({ callback!!.onSnapToRoadRetrieved(snappedPointList) })

    }

    private fun onError(error: RequestError?) {
        if (error != null) {
            Logger.getInstance()!!.logE(tag, "Use case returned error: err=" + error, DebugMessage.TYPE_USE_CASE)
            mainHandler.post({ callback!!.onError(error) })
        } else {
            Logger.getInstance()!!.logE(tag, "Use case returned error: err="
                    + RequestError.getUnknownError(), DebugMessage.TYPE_USE_CASE)
            mainHandler.post({ callback!!.onError(RequestError.getUnknownError()) })
        }
    }

}