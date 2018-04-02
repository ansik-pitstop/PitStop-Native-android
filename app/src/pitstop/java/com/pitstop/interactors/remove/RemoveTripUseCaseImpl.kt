package com.pitstop.interactors.remove

import android.os.Handler
import android.util.Log
import com.pitstop.models.DebugMessage
import com.pitstop.network.RequestError
import com.pitstop.repositories.TripRepository
import com.pitstop.utils.Logger
import io.reactivex.schedulers.Schedulers

/**
 * Created by David C. on 26/3/18.
 */
class RemoveTripUseCaseImpl(private val tripRepository: TripRepository,
                            private val useCaseHandler: Handler,
                            private val mainHandler: Handler) : RemoveTripUseCase {

    private val tag = javaClass.simpleName
    private var tripId: String = ""
    private var vin: String = ""
    private var callback: RemoveTripUseCase.Callback? = null

    override fun execute(tripId: String, vin: String, callback: RemoveTripUseCase.Callback) {
        Logger.getInstance()!!.logI(tag, "Use case execution started", DebugMessage.TYPE_USE_CASE)
        this.tripId = tripId
        this.vin = vin
        this.callback = callback
        useCaseHandler.post(this)
    }

    override fun run() {

        Log.d(tag, "run()")

        tripRepository.deleteTrip(tripId, vin)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation())
                .subscribe({ next ->
                    Log.d(tag, "tripRepository.onNext() data: $next")
                    this@RemoveTripUseCaseImpl.onTripRemoved()
                }, { error ->
                    Log.d(tag, "tripRepository.onError() error: " + error)
                    this@RemoveTripUseCaseImpl.onError(com.pitstop.network.RequestError(error))
                })

    }

    private fun onTripRemoved() {

        Logger.getInstance()!!.logI(tag, "Use case finished: trip removed", DebugMessage.TYPE_USE_CASE)
        mainHandler.post { callback!!.onTripRemoved() }

    }

    private fun onError(err: RequestError?) {
        if (err != null) {
            Logger.getInstance()!!.logE(tag, "Use case returned error: err=" + err, DebugMessage.TYPE_USE_CASE)
            mainHandler.post({ callback!!.onError(err) })
        } else {
            Logger.getInstance()!!.logE(tag, "Use case returned error: err="
                    + RequestError.getUnknownError(), DebugMessage.TYPE_USE_CASE)
            mainHandler.post({ callback!!.onError(RequestError.getUnknownError()) })
        }
    }
}