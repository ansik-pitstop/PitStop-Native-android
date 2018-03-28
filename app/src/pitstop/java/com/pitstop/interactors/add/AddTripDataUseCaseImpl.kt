package com.pitstop.interactors.add

import android.location.Location
import android.os.Handler
import android.util.Log
import com.pitstop.models.DebugMessage
import com.pitstop.models.Settings
import com.pitstop.models.trip.LocationData
import com.pitstop.models.trip.PendingLocation
import com.pitstop.models.trip.TripData
import com.pitstop.network.RequestError
import com.pitstop.repositories.CarRepository
import com.pitstop.repositories.Repository
import com.pitstop.repositories.TripRepository
import com.pitstop.repositories.UserRepository
import com.pitstop.utils.Logger
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

/**
 * Created by Karol Zdebel on 3/6/2018.
 */
class AddTripDataUseCaseImpl(private val tripRepository: TripRepository
                             , private val userRepository: UserRepository, private val carRepository: CarRepository
                             , private val useCaseHandler: Handler
                             , private val mainHandler: Handler): AddTripDataUseCase {

    private val TAG = javaClass.simpleName
    private lateinit var locationList: List<Location>
    private lateinit var callback: AddTripDataUseCase.Callback


    override fun execute(locationList: List<Location>, callback: AddTripDataUseCase.Callback) {
        Logger.getInstance().logI(TAG, "Use case execution started, trip: " + locationList, DebugMessage.TYPE_USE_CASE)
        this.locationList = arrayListOf()
        (this.locationList as ArrayList<Location>).addAll(locationList)
        this.callback = callback
        useCaseHandler.post(this)
    }

    override fun run() {
        Log.i(TAG,"AddTripUseCaseImpl: run(), trip.size ${locationList.size}")

        val trip = arrayListOf<PendingLocation>()
        locationList.forEach({trip.add(PendingLocation(it.longitude,it.latitude,it.time))})

        userRepository.getCurrentUserSettings(object: Repository.Callback<Settings>{
            override fun onSuccess(data: Settings?) {
                System.out.println("AddTripUseCaseImpl: Got settings with carId: ${data!!.carId}")
                Log.d(TAG,"got settings with carId: ${data!!.carId}")
                carRepository.get(data!!.carId)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.from(useCaseHandler.looper))
                        .subscribe({ car ->
                            if (car.isLocal) return@subscribe

                            val locationDataList: MutableSet<LocationData> = hashSetOf()
                            trip.forEach{ locationDataList.add(LocationData(trip[0].time, it)) }

                            val tripIdFromRepo = tripRepository.getIncompleteTripId()
                            //First set of locations for this trip, set trip id its not in db yet, or use the retrieved if not -1
                            val tripId = if (tripIdFromRepo == -1L) trip.first().time else tripIdFromRepo

                            tripRepository.storeTripData(TripData(tripId,false, car.data!!.vin
                                    , locationDataList))
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(Schedulers.io())
                                    .subscribe({next ->
                                        Log.d(TAG,"trip repo response: $next")
                                        AddTripUseCaseImpl@onAddedTrip()
                                    }, {err ->
                                        Log.d(TAG,"trip repo err: $err")
                                        AddTripUseCaseImpl@onErrorFound(RequestError(err))
                                    })

                        }, { err ->
                            Log.d(TAG, "Error: " + err)
                            AddTripUseCaseImpl@onErrorFound(RequestError(err))
                        })
            }
            override fun onError(error: RequestError?) {
                AddTripUseCaseImpl@onErrorFound(error ?: RequestError.getUnknownError())
            }
        })
    }

    private fun onAddedTrip() {
        Logger.getInstance().logI(TAG, "Use case finished: trip added!", DebugMessage.TYPE_USE_CASE)
        mainHandler.post({callback.onAddedTripData()})
    }

    private fun onErrorFound(err: RequestError){
        Logger.getInstance().logE(TAG, "Use case returned error: "+err.message, DebugMessage.TYPE_USE_CASE)
        mainHandler.post({callback.onError(err)})
    }
}