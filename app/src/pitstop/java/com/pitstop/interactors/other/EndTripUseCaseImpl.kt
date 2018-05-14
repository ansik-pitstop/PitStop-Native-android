package com.pitstop.interactors.other

import android.os.Handler
import android.util.Log
import com.pitstop.interactors.other.EndTripUseCase.Companion.MIN_CONF
import com.pitstop.models.DebugMessage
import com.pitstop.models.Settings
import com.pitstop.models.sensor_data.trip.LocationData
import com.pitstop.models.sensor_data.trip.PendingLocation
import com.pitstop.models.sensor_data.trip.TripData
import com.pitstop.models.trip.RecordedLocation
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
    private var locationList: MutableList<RecordedLocation> = arrayListOf()

    override fun execute(trip: List<RecordedLocation>, callback: EndTripUseCase.Callback) {
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
            trip.add(PendingLocation(it.longitude, it.latitude, it.time / 1000, it.conf))
        })

        //Get incomplete data before completing it so that it can be used for checking trip validity
        tripRepository.getIncompleteTripData()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe({incompleteTripData ->

                    //Incomplete list of locations
                    val locationDataList: MutableSet<LocationData> = hashSetOf()
                    trip.forEach { locationDataList.add(LocationData(it.time, it)) }

                    //Check for minimum confidence in both memory and local storage location points
                    if (incompleteTripData.locations.find { it.data.confidence >= MIN_CONF } == null
                            && locationDataList.find { it.data.confidence >= MIN_CONF } == null){

                        //Remove trip since it doesn't have a point with a min conf of 70
                        Log.d(TAG,"no trip point with min confidence of 70")
                        tripRepository.removeIncompleteTripData()
                        EndTripUseCaseImpl@finishedTripDiscarded()
                        return@subscribe
                    }
                    //Complete trip data in the trip repo
                    else tripRepository.completeTripData()
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

                                                    Log.d(TAG,"proceeding to get tripIdFromRepo, got: ${incompleteTripData.id}, trip.size: ${trip.size}")
                                                    //First set of locations for this locationList, set locationList id its not in db yet, or use the retrieved if not -1
                                                    val tripId = if (incompleteTripData.id == -1L) trip.firstOrNull()?.time ?: incompleteTripData.id else incompleteTripData.id

                                                    Log.d(TAG,"locationDataList: $locationDataList, incompleteTripData: $it")
                                                        Log.d(TAG,"found min confidence > $MIN_CONF")
                                                    tripRepository.storeTripDataAndDump(TripData(tripId, true, car.data!!.vin
                                                            , locationDataList))
                                                            .subscribeOn(Schedulers.io())
                                                            .observeOn(Schedulers.io())
                                                            .subscribe({next ->
                                                                Log.d(TAG,"trip repo response: $next")
                                                                EndTripUseCaseImpl@finished(next)
                                                            }, {err ->
                                                                Log.d(TAG,"trip repo err: $err")
                                                                AddTripUseCaseImpl@onErrorFound(RequestError(err))
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
                },{error ->
                    EndTripUseCaseImpl@onErrorFound(RequestError(error))
                })
    }

    private fun finishedTripDiscarded(){
        Logger.getInstance()!!.logW(TAG
                , "Use case finished: trip was discarded since no location point met minimum confidence"
                , DebugMessage.TYPE_USE_CASE)
        mainHandler.post{callback.tripDiscarded()}
    }

    private fun finished(rows: Int){
        Logger.getInstance()!!.logI(TAG
                , "Use case finished: success rows = $rows", DebugMessage.TYPE_USE_CASE)
        mainHandler.post{callback.finished()}
    }

    private fun onErrorFound(err: RequestError){
        Logger.getInstance().logE(TAG, "Use case returned error: $err", DebugMessage.TYPE_USE_CASE)
        mainHandler.post({callback.onError(err)})
    }
}