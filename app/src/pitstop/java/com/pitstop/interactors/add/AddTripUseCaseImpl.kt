package com.pitstop.interactors.add

import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Handler
import android.util.Log
import com.pitstop.models.DebugMessage
import com.pitstop.models.Settings
import com.pitstop.models.trip.LocationData
import com.pitstop.models.trip.TripData
import com.pitstop.network.RequestError
import com.pitstop.repositories.CarRepository
import com.pitstop.repositories.Repository
import com.pitstop.repositories.TripRepository
import com.pitstop.repositories.UserRepository
import com.pitstop.utils.Logger
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.IOException

/**
 * Created by Karol Zdebel on 3/6/2018.
 */
class AddTripUseCaseImpl(private val geocoder: Geocoder, private val tripRepository: TripRepository
                         ,private val userRepository: UserRepository , private val carRepository: CarRepository
                         , private val useCaseHandler: Handler
                         , private val mainHandler: Handler): AddTripUseCase {

    private val TAG = javaClass.simpleName
    private lateinit var trip: List<Location>
    private lateinit var callback: AddTripUseCase.Callback


    override fun execute(trip: List<Location>, callback: AddTripUseCase.Callback) {
        Logger.getInstance().logI(TAG, "Use case execution started, trip: " + trip, DebugMessage.TYPE_USE_CASE)
        this.trip = arrayListOf()
        (this.trip as ArrayList<Location>).addAll(trip)
        this.callback = callback
        useCaseHandler.post(this)
    }

    override fun run() {
        Log.i(TAG,"AddTripUseCaseImpl: run(), trip.size ${trip.size}")
        var startAddress: Address? = null
        var endAddress: Address? = null
        try{
            startAddress = geocoder
                    .getFromLocation(trip.first().latitude,trip.first().longitude,1).firstOrNull()
            endAddress = geocoder
                    .getFromLocation(trip.last().latitude,trip.last().longitude,1).firstOrNull()
        }catch (e: IOException){
            e.printStackTrace()
        }

        Log.i(TAG,"AddTripUseCaseImpl: startAddress: $startAddress endAddress: $endAddress")

        val tripDataPoints: MutableSet<LocationData> = mutableSetOf()

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


                            tripRepository.storeTripData(TripData(trip.first().time, car.data!!.vin
                                    , System.currentTimeMillis(), locationDataList))
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
        tripRepository.localTripStorage.store(trip)
    }

    private fun onAddedTrip() {
        Logger.getInstance().logI(TAG, "Use case finished: trip added!", DebugMessage.TYPE_USE_CASE)
        mainHandler.post({callback.onAddedTrip()})
    }

    private fun onErrorFound(err: RequestError){
        Logger.getInstance().logE(TAG, "Use case returned error: "+err.message, DebugMessage.TYPE_USE_CASE)
        mainHandler.post({callback.onError(err)})
    }
}