package com.pitstop.interactors.get

import android.os.Handler
import android.util.Log
import com.pitstop.models.DebugMessage
import com.pitstop.models.trip.Trip
import com.pitstop.network.RequestError
import com.pitstop.repositories.TripRepository
import com.pitstop.utils.Logger
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

/**
 * Created by David C. on 12/3/18.
 */
class GetTripsUseCaseImpl(private val tripRepository: TripRepository,
                          private val useCaseHandler: Handler,
                          private val mainHandler: Handler) : GetTripsUseCase {

    private val tag = javaClass.simpleName
    private var callback: GetTripsUseCase.Callback? = null
    private var vin: String = ""

    override fun execute(vin: String, callback: GetTripsUseCase.Callback) {
        Logger.getInstance()!!.logI(tag, "Use case execution started", DebugMessage.TYPE_USE_CASE)
        this.vin = vin
        this.callback = callback
        useCaseHandler.post(this)
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

    private fun onTripsRetrieved(tripList: List<Trip>) {

        Logger.getInstance()!!.logI(tag, "Use case finished result: trips=$tripList", DebugMessage.TYPE_USE_CASE)
        mainHandler.post({ callback!!.onTripsRetrieved(tripList, isLocal = true) })

    }

    override fun run() {
        Log.d(tag, "run()")

        tripRepository.getTripsByCarVin("WVWXK73C37E116278") //TODO: replace with the current Car's VIN
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.from(useCaseHandler.looper))
                .subscribe({ next ->
                    Log.d(tag, "tripRepository.onNext() data: " + next)
                    this@GetTripsUseCaseImpl.onTripsRetrieved(next.data.orEmpty())
                }, { error ->
                    Log.d(tag, "tripRepository.onErrorResumeNext() error: " + error)
                    this@GetTripsUseCaseImpl.onError(com.pitstop.network.RequestError(error))
                })

    }

}