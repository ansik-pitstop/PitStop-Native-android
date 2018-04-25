package com.pitstop.interactors.other

import android.location.Location
import android.os.Handler
import android.util.Log
import com.pitstop.models.DebugMessage
import com.pitstop.models.Settings
import com.pitstop.models.sensor_data.trip.LocationData
import com.pitstop.models.sensor_data.trip.PendingLocation
import com.pitstop.models.sensor_data.trip.TripData
import com.pitstop.network.RequestError
import com.pitstop.repositories.CarRepository
import com.pitstop.repositories.Repository
import com.pitstop.repositories.TripRepository
import com.pitstop.repositories.UserRepository
import com.pitstop.utils.Logger
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

/**
 * Created by Karol Zdebel on 3/28/2018.
 */
class EndTripUseCaseImpl(private val userRepository: UserRepository
                         , private val carRepository: CarRepository
                         , private val tripRepository: TripRepository
                         , private val usecaseHandler: Handler
                         , private val mainHandler: Handler): EndTripUseCase {

    private val TAG = javaClass.simpleName
    private lateinit var callback: EndTripUseCase.Callback
    private var locationList: MutableList<Location> = arrayListOf()

    override fun execute(trip: List<Location>, callback: EndTripUseCase.Callback) {
        Logger.getInstance()!!.logI(TAG, "Use case started execution, trip.size: ${trip.size}"
                , DebugMessage.TYPE_USE_CASE)
        this.locationList.addAll(trip)
        this.callback = callback
        usecaseHandler.post(this)
    }

    override fun run() {

        val trip = arrayListOf<PendingLocation>()
        //Location stores time in ms, we want seconds
        locationList.forEach({
            trip.add(PendingLocation(it.longitude, it.latitude, it.time / 1000))
        })

        //Get the tripId of the incomplete trip in local database before marking these rows completed
        //We will be unable to retrieve tripid after that becasue we do not know which the current trip is
        val tripIdFromRepo = tripRepository.getIncompleteTripId()

        //Complete trip data in the trip repo
        tripRepository.completeTripData()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.from(usecaseHandler.looper))
                .subscribe({
                    Log.d(TAG,"data marked as completed!")
                    //Insert remaining completed trips
                    userRepository.getCurrentUserSettings(object: Repository.Callback<Settings> {

                        override fun onError(error: RequestError?) {
                            if (error == null){
                                onError(RequestError.getUnknownError())
                            }else{
                                onErrorFound(error)
                            }
                        }

                        override fun onSuccess(data: Settings?) {
                            Log.d(TAG, "got settings with carId: ${data!!.carId}")
                            var usedLocalCar = false

                            carRepository.get(data!!.carId)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.from(usecaseHandler.looper),true)
                                    .subscribe (carRepoSubscribe@{ car ->

                                        //Use local response if it has data otherwise use remote
                                        if (car.isLocal && car.data != null){
                                            usedLocalCar = true
                                        }else if (usedLocalCar){
                                            return@carRepoSubscribe
                                        }

                                        Log.d(TAG,"proceeding to get tripIdFromRepo, got: $tripIdFromRepo, trip.size: ${trip.size}")
                                        //First set of locations for this locationList, set locationList id its not in db yet, or use the retrieved if not -1
                                        val tripId = if (tripIdFromRepo == -1L) trip.firstOrNull()?.time ?: tripIdFromRepo else tripIdFromRepo

                                        //Incomplete list of locations
                                        val locationDataList: MutableSet<LocationData> = hashSetOf()
                                        trip.forEach { locationDataList.add(LocationData(it.time, it)) }

                                        Log.d(TAG,"Storing trip data")
                                        tripRepository.storeTripDataAndDump(TripData(tripId, true, car.data!!.vin
                                                , locationDataList))
                                                .subscribeOn(Schedulers.io())
                                                .observeOn(Schedulers.io())
                                                .subscribe({ next ->
                                                    finished(it)
                                                }, { err ->
                                                    Log.d(TAG, "Error: " + err)
                                                    onErrorFound(RequestError(err))
                                                })

                                    }, { err ->
                                        Log.d(TAG, "Error: " + err)
                                        onErrorFound(RequestError(err))
                                    })
                        }
                    })

                },{
                    it.printStackTrace()
                    onErrorFound(RequestError(it))
                })
    }

    private fun finished(rows: Int){
        Logger.getInstance()!!.logI(TAG
                , "Use case finished: success rows = $rows", DebugMessage.TYPE_USE_CASE)
        mainHandler.post{callback.finished()}
    }

    private fun onErrorFound(err: RequestError){
        Logger.getInstance().logE(TAG, "Use case returned error: $err.message", DebugMessage.TYPE_USE_CASE)
        mainHandler.post({callback.onError(err)})
    }
}