package com.pitstop.interactors.other

import android.os.Handler
import android.util.Log
import com.google.android.gms.location.DetectedActivity
import com.pitstop.database.LocalActivityStorage
import com.pitstop.database.LocalLocationStorage
import com.pitstop.models.DebugMessage
import com.pitstop.models.sensor_data.trip.LocationData
import com.pitstop.models.sensor_data.trip.PendingLocation
import com.pitstop.models.sensor_data.trip.TripData
import com.pitstop.models.trip.CarLocation
import com.pitstop.repositories.TripRepository
import com.pitstop.utils.Logger
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers

/**
 * Created by Karol Zdebel on 6/4/2018.
 */
class ProcessTripDataUseCaseImpl(private val localLocationStorage: LocalLocationStorage
                                 , private val localActivityStorage: LocalActivityStorage
                                 , private val tripRepository: TripRepository
                                 , private val usecaseHandler: Handler
                                 , private val mainHandler: Handler): ProcessTripDataUseCase {

    private val tag = ProcessTripDataUseCaseImpl::class.java.simpleName
    private lateinit var callback: ProcessTripDataUseCase.Callback

    private val LOW_FOOT_CONF = 40
    private val LOW_VEH_CONF = 30
    private val HIGH_VEH_CONF = 70
    private val HIGH_FOOT_CONF = 90
    private val HIGH_STILL_CONF = 99
    private val STILL_TIMEOUT = 600000

    private var hardStart = -1L
    private var softStart = -1L
    private var hardEnd = -1L
    private var softEnd = -1L

    override fun execute(callback: ProcessTripDataUseCase.Callback) {
        this.callback = callback
        Logger.getInstance().logI(tag,"Use case execution started"
                ,DebugMessage.TYPE_USE_CASE)
        usecaseHandler.post(this)
    }

    override fun run() {
        val locations = localLocationStorage.getAll()
        val activities = localActivityStorage.getAll()

        val processedTrips = arrayListOf<List<CarLocation>>()

        activities.forEach loop@{

            //See how long we've been still for, if at all
            if (softEnd != -1L && it.time - softEnd > STILL_TIMEOUT){
                Logger.getInstance().logI(tag,"soft end end time=${it.time}"
                        ,DebugMessage.TYPE_USE_CASE)
                hardEnd = it.time

                //Process trip location points
                if (hardStart != -1L){
                    val trip = arrayListOf<CarLocation>()
                    locations.forEach {
                        if (it.time in softStart..softEnd){
                            trip.add(it)
                        }
                    }
                    processedTrips.add(trip)

                    //Remove all processed data points
                    val removedLocs = localLocationStorage.remove(locations.filter { it.time <= hardEnd })
                    val removedActivities = localActivityStorage.remove(activities.filter {it.time <= hardEnd})

                    Logger.getInstance().logD(tag,"Removed $removedLocs locations and " +
                            "$removedActivities activities after processing trip",DebugMessage.TYPE_TRIP)

                    //Reset variables in case another trip is present
                    softStart = -1L
                    softEnd = -1L
                    hardStart = -1L
                    hardEnd = -1L
                }
            }

            when(it.type){
                (DetectedActivity.IN_VEHICLE) -> {

                    //Hard start
                    if (it.conf >= HIGH_VEH_CONF
                            && (hardStart == -1L || softStart == -1L || softEnd != -1L)){
                        if (hardStart == -1L) hardStart = it.time
                        if (softStart == -1L) softStart = it.time
                        softEnd = -1
                        Logger.getInstance().logI(tag,"Hard start time=${it.time}" +
                                ", hardstart = $hardStart, softStart=$softStart, softEnd=$softEnd"
                                ,DebugMessage.TYPE_USE_CASE)
                    }
                    //End soft still end or soft start
                    else if (it.conf >= LOW_VEH_CONF &&
                            (softEnd != -1L || (softStart == -1L && hardStart == -1L) ) ){
                        //See if walking confidence is less than 40 for that time or null
                        val footActivity = activities.find{a -> a.time == it.time
                                && it.type == DetectedActivity.ON_FOOT}
                        if (footActivity == null || footActivity.conf < LOW_FOOT_CONF){
                            //Soft start

                            //Set soft start if it already wasn't set, we might just be resetting softEnd here
                            if (softStart == -1L) softStart = it.time
                            softEnd = -1
                            Logger.getInstance().logD(tag,"Soft start time=${it.time}"
                                    ,DebugMessage.TYPE_USE_CASE)
                        }
                    }
                }
                (DetectedActivity.ON_FOOT) -> {
                    if (hardStart != -1L && it.conf > HIGH_FOOT_CONF){
                        hardEnd = it.time

                        //Process trip location points
                        if (hardStart != -1L){
                            Logger.getInstance().logD(tag,"Hard end time=${it.time}"
                                    ,DebugMessage.TYPE_USE_CASE)

                            val trip = arrayListOf<CarLocation>()
                            locations.forEach {
                                if (it.time in softStart..hardEnd){
                                    trip.add(it)
                                }
                            }
                            processedTrips.add(trip)

                            //Remove all processed data points
                            val removedLocs = localLocationStorage.remove(locations.filter { it.time <= hardEnd })
                            val removedActivities = localActivityStorage.remove(activities.filter {it.time <= hardEnd})

                            Logger.getInstance().logD(tag,"Removed $removedLocs locations and " +
                                    "$removedActivities activities after processing trip",DebugMessage.TYPE_TRIP)

                            //Reset variables in case another trip is present
                            softStart = -1L
                            softEnd = -1L
                            hardStart = -1L
                            hardEnd = -1L
                        }
                    }
                }
                (DetectedActivity.STILL) -> {
                    if (it.conf >= HIGH_STILL_CONF && (softStart != -1L || hardStart != -1L)){
                        if (softEnd == -1L){
                            softEnd = it.time
                            Logger.getInstance().logD(tag,"Soft end start found time=${it.time}"
                                    ,DebugMessage.TYPE_USE_CASE)
                        }
                    }
                }
            }
        }

        val observableList = arrayListOf<Observable<Int>>()

        Log.d(tag,"processedTrips: size=${processedTrips.size} data=$processedTrips")

        processedTrips.filter { !it.isEmpty() }.forEach({
            val recordedLocationList = mutableSetOf<LocationData>()
            it.forEach { carLocation ->
                recordedLocationList.add(LocationData(carLocation.time/1000, PendingLocation(carLocation.longitude
                        ,carLocation.latitude,carLocation.time/1000)))
            }
            Log.d(tag,"recorded location list: $recordedLocationList")
            val tripData = TripData(it[0].time/1000,it[0].vin,recordedLocationList)
            observableList.add(tripRepository.storeTripDataAndDump(tripData))
        })

        Logger.getInstance().logD(tag,"observable list size: ${observableList.size}"
                ,DebugMessage.TYPE_USE_CASE)

        if (observableList.isEmpty()){

            //Remove all data before soft start, if none exists then remove all data
            //since a trip cannot be produced, a hard start also sets the soft start variable
            var end = Long.MAX_VALUE
            if (softStart != -1L)
                end = softStart
            val removedLocs = localLocationStorage.remove(locations.filter { it.time < end})
            val removedActivities = localActivityStorage.remove(activities.filter {it.time < end})
            Logger.getInstance().logD(tag,"Removed $removedLocs locations and " +
                    "$removedActivities activities after processing trip",DebugMessage.TYPE_TRIP)

            mainHandler.post({
                callback.processed(processedTrips)
            })
        }
        else Observable.combineLatest(observableList, {
            it.sumBy { 1 } //add one per trip stored
        }).subscribeOn(Schedulers.computation())
        .observeOn(Schedulers.io(),true)
        .subscribe({next ->
            Logger.getInstance().logI(tag, "Processed trips, combine latest response: $next"
                    ,DebugMessage.TYPE_USE_CASE)
            mainHandler.post({
                callback.processed(processedTrips)
            })
        },{err ->
            Logger.getInstance().logI(tag, "Processed trips, combine latest response: $err"
                    , DebugMessage.TYPE_USE_CASE)
            mainHandler.post({
                callback.processed(processedTrips)
            })
        })


    }
}