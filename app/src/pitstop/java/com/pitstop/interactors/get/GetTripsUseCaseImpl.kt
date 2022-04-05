package com.pitstop.interactors.get

import android.os.Handler
import android.util.Log
import com.pitstop.application.Constants
import com.pitstop.models.DebugMessage
import com.pitstop.models.Settings
import com.pitstop.models.trip.Trip
import com.pitstop.network.RequestError
import com.pitstop.repositories.*
import com.pitstop.utils.Logger
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

/**
 * Created by David C. on 12/3/18.
 */
class GetTripsUseCaseImpl(private val userRepository: UserRepository,
                          private val carRepository: CarRepository,
                          private val tripRepository: TripRepository,
                          private val useCaseHandler: Handler,
                          private val mainHandler: Handler) : GetTripsUseCase {

    private val tag = javaClass.simpleName
    private var callback: GetTripsUseCase.Callback? = null
    private val compositeDisposable = CompositeDisposable()
    private var carId: Int = 0

    override fun execute(carId: Int, callback: GetTripsUseCase.Callback) {
        Logger.getInstance()!!.logI(tag, "Use case execution started", DebugMessage.TYPE_USE_CASE)
        this.carId = carId
        this.callback = callback
        useCaseHandler.post(this)
    }

    private fun onError(err: RequestError?) {
        compositeDisposable.clear()
        if (err != null) {
            Logger.getInstance()!!.logE(tag, "Use case returned error: err=" + err, DebugMessage.TYPE_USE_CASE)
            mainHandler.post({ callback!!.onError(err) })
        } else {
            Logger.getInstance()!!.logE(tag, "Use case returned error: err="
                    + RequestError.getUnknownError(), DebugMessage.TYPE_USE_CASE)
            mainHandler.post({ callback!!.onError(RequestError.getUnknownError()) })
        }
    }

    private fun onNoCar() {
        compositeDisposable.clear()
        Logger.getInstance()!!.logI(tag, "Use case finished result: no car added", DebugMessage.TYPE_USE_CASE)
        mainHandler.post({ callback!!.onNoCar() })
    }

    private fun onTripsRetrieved(tripList: List<Trip>, isLocal: Boolean) {
        if (!isLocal){
            compositeDisposable.clear()
        }
        Logger.getInstance()!!.logI(tag, "Use case finished result: trips=$tripList", DebugMessage.TYPE_USE_CASE)
        mainHandler.post({ callback!!.onTripsRetrieved(tripList, isLocal) })

    }

    override fun run() {
        Log.d(tag, "run()")
        val disposable = carRepository.get(this.carId, Repository.DATABASE_TYPE.BOTH)
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.io(), true)
                .subscribe({ car ->
                    if (car.data == null) return@subscribe
                    Log.d(tag, "got car vin: ${car.data.vin}, isLocal = ${car.isLocal}")

                    var whatToReturn: String = Constants.TRIP_REQUEST_REMOTE
//                    if (car.isLocal) {
//                        whatToReturn = Constants.TRIP_REQUEST_LOCAL
//                    } else {
//                        whatToReturn = Constants.TRIP_REQUEST_REMOTE
//                    }

//                    tripRepository.getTripsByCarVin1(car.data.vin, whatToReturn)

                    val disposable = tripRepository.getTripsByCarVin(car.data.vin, whatToReturn)
                            .subscribeOn(Schedulers.computation())
                            .observeOn(Schedulers.io(), true)
                            .subscribe({ next ->
                                Log.d(tag, "tripRepository.onNext()                                                                                                                                    data: $next , isLocal = ${next.isLocal}")
                                this@GetTripsUseCaseImpl.onTripsRetrieved(next.data.orEmpty(), next.isLocal)
                            }, { error ->
                                Log.d(tag, "tripRepository.onErrorResumeNext() error: " + error)
                                this@GetTripsUseCaseImpl.onError(RequestError(error))
                            })
                    compositeDisposable.add(disposable)

                }, { err ->
                    Log.d(tag, "Error: " + err)
                    err.printStackTrace();
                    this@GetTripsUseCaseImpl.onError(RequestError(err))
                })
        compositeDisposable.add(disposable)
    }

}