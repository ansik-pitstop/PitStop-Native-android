package com.pitstop.interactors.get

import android.os.Handler
import android.util.Log
import com.pitstop.application.Constants
import com.pitstop.models.DebugMessage
import com.pitstop.models.Settings
import com.pitstop.models.trip.Trip
import com.pitstop.network.RequestError
import com.pitstop.repositories.CarRepository
import com.pitstop.repositories.Repository
import com.pitstop.repositories.TripRepository
import com.pitstop.repositories.UserRepository
import com.pitstop.utils.Logger
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

    override fun execute(callback: GetTripsUseCase.Callback) {
        Logger.getInstance()!!.logI(tag, "Use case execution started", DebugMessage.TYPE_USE_CASE)
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

    private fun onNoCar() {

        Logger.getInstance()!!.logI(tag, "Use case finished result: no car added", DebugMessage.TYPE_USE_CASE)
        mainHandler.post({ callback!!.onNoCar() })

    }

    private fun onTripsRetrieved(tripList: List<Trip>, isLocal: Boolean) {

        Logger.getInstance()!!.logI(tag, "Use case finished result: trips=$tripList", DebugMessage.TYPE_USE_CASE)
        mainHandler.post({ callback!!.onTripsRetrieved(tripList, isLocal) })

    }

    override fun run() {
        Log.d(tag, "run()")

        userRepository.getCurrentUserSettings(object : Repository.Callback<Settings> {
            override fun onSuccess(data: Settings?) {
                Log.d(tag, "got settings with carId: ${data!!.carId}")
                if (!data.hasMainCar()) onNoCar()
                else carRepository.get(data.carId)
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.computation(), true)
                        .subscribe({ car ->
                            if (car.data == null) return@subscribe
                            Log.d(tag, "got car vin: ${car.data.vin}, isLocal = ${car.isLocal}")

                            var whatToReturn: String
                            if (car.isLocal) {
                                whatToReturn = Constants.TRIP_REQUEST_LOCAL
                            } else {
                                whatToReturn = Constants.TRIP_REQUEST_REMOTE
                            }

                            tripRepository.getTripsByCarVin(car.data.vin, whatToReturn)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(Schedulers.computation(), true)
                                    .subscribe({ next ->
                                        Log.d(tag, "tripRepository.onNext() data: $next , isLocal = ${next.isLocal}")
                                        this@GetTripsUseCaseImpl.onTripsRetrieved(next.data.orEmpty(), next.isLocal)
                                    }, { error ->
                                        Log.d(tag, "tripRepository.onErrorResumeNext() error: " + error)
                                        this@GetTripsUseCaseImpl.onError(RequestError(error))
                                    })

                        }, { err ->
                            Log.d(tag, "Error: " + err)
                            this@GetTripsUseCaseImpl.onError(RequestError(err))
                        })
            }

            override fun onError(error: RequestError?) {
                this@GetTripsUseCaseImpl.onError(error)
            }
        })

    }

}