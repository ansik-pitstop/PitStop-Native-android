package com.pitstop.interactors.other

import android.os.Handler
import android.util.Log
import com.pitstop.database.LocalActivityStorage
import com.pitstop.database.LocalLocationStorage
import com.pitstop.models.DebugMessage
import com.pitstop.models.sensor_data.trip.LocationData
import com.pitstop.models.sensor_data.trip.PendingLocation
import com.pitstop.models.sensor_data.trip.TripData
import com.pitstop.models.trip.CarActivity
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

    companion object {
        val LOW_FOOT_CONF = 40
        val LOW_VEH_CONF = 30
        val HIGH_VEH_CONF = 70
        val HIGH_FOOT_CONF = 90
        val HIGH_STILL_CONF = 99
        val STILL_TIMEOUT = 600000
        val HARD_END_TIME_OFFSET = 1000 * 60 * 15 //Time after a trip ends that a location still qualifies as within the trip
        val BEFORE_START_TIME_OFFSET = 1000 * 60 * 15 //Time before a trip starts that a location still qualifies as within the trip
    }

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
            if ( (hardStart != -1L || softStart != -1L)
                    && softEnd != -1L && it.time - softEnd > STILL_TIMEOUT){
                Logger.getInstance().logD(tag,"soft end end time=${it.time}"
                        ,DebugMessage.TYPE_USE_CASE)
                hardEnd = it.time

                //Process trip location points
                if (hardStart != -1L){
                    processedTrips.add(filterLocations(softStart,softEnd,locations))

                    //Remove all processed data points
                    val removedLocs = localLocationStorage.remove(locations.filter { it.time <= hardEnd })
                    val removedActivities = localActivityStorage.remove(activities.filter {it.time <= hardEnd})

                    Logger.getInstance().logD(tag,"Removed $removedLocs locations and " +
                            "$removedActivities activities after processing trip",DebugMessage.TYPE_TRIP)
                }

                //Reset variables regardless of softStart or hardStart
                softStart = -1L
                softEnd = -1L
                hardStart = -1L
                hardEnd = -1L
            }

            when(it.type){
                (CarActivity.TYPE_MANUAL_START) -> {
                    if (hardStart == -1L) hardStart = it.time
                    if (softStart == -1L) softStart = it.time
                    softEnd = -1
                    Log.d(tag,"manual trip start found, time = ${it.time}")
                }
                (CarActivity.TYPE_MANUAL_END) -> {
                    Log.d(tag,"manual end found, time = ${it.time}, hardstart = ${it.time}")
                    hardEnd = it.time
                    processedTrips.add(filterLocations(softStart,hardEnd,locations))

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
                (CarActivity.TYPE_DRIVING) -> {

                    //Hard start
                    if (it.conf >= HIGH_VEH_CONF
                            && (hardStart == -1L || softStart == -1L || softEnd != -1L)){
                        if (hardStart == -1L) hardStart = it.time
                        if (softStart == -1L) softStart = it.time
                        softEnd = -1
                        Logger.getInstance().logD(tag,"Hard start time=${it.time}" +
                                ", hardstart = $hardStart, softStart=$softStart, softEnd=$softEnd"
                                ,DebugMessage.TYPE_USE_CASE)
                    }
                    //End soft still end or soft start
                    else if (it.conf >= LOW_VEH_CONF &&
                            (softEnd != -1L || (softStart == -1L && hardStart == -1L) ) ){
                        //See if walking confidence is less than 40 for that time or null
                        val footActivity = activities.find{a -> a.time == it.time
                                && it.type == CarActivity.TYPE_ON_FOOT}
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
                (CarActivity.TYPE_ON_FOOT) -> {
                    if (hardStart != -1L && it.conf > HIGH_FOOT_CONF){
                        hardEnd = it.time

                        //Process trip location points
                        if (hardStart != -1L){
                            Logger.getInstance().logD(tag,"Hard end time=${it.time}"
                                    ,DebugMessage.TYPE_USE_CASE)


                            processedTrips.add(filterLocations(softStart,hardEnd,locations))

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
                (CarActivity.TYPE_STILL) -> {
                    if (it.conf >= HIGH_STILL_CONF && softEnd == -1L
                            && (softStart != -1L || hardStart != -1L)){
                        softEnd = it.time
                        Logger.getInstance().logD(tag,"Soft end start found time=${it.time}"
                                ,DebugMessage.TYPE_USE_CASE)
                    }
                }
            }
        }

        val observableList = arrayListOf<Observable<Int>>()

        Log.d(tag,"processedTrips: size=${processedTrips.size} data=$processedTrips")

        processedTrips.filter { !it.isEmpty() }.forEach({
            val recordedLocationList = mutableListOf<LocationData>()
            it.forEachIndexed { i,carLocation ->
                //Do not include points in the same location back to back
                if ((it.lastIndex != i && (it[i+1].latitude != carLocation.latitude
                        || it[i+1].longitude != carLocation.longitude ) ) || it.lastIndex == i){
                    recordedLocationList.add(LocationData(carLocation.time/1000, PendingLocation(carLocation.longitude
                            ,carLocation.latitude,carLocation.time/1000)))
                }
            }
            Log.d(tag,"recorded location list: $recordedLocationList")
            val tripData = TripData(it[0].time/1000,it[0].vin,recordedLocationList
                    , (softStart/1000).toInt(),(hardEnd/1000).toInt())
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
            Logger.getInstance().logI(tag, "Processed trips, empty"
                    ,DebugMessage.TYPE_USE_CASE)
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

    private fun filterLocations(start: Long, end: Long, locations: List<CarLocation>): List<CarLocation>{
        val trip = arrayListOf<CarLocation>()
        var includedPriorLoc = false
        var includedAfterLoc = false
        //Locations sorte by time
        locations.forEach {
            if (it.time in start-BEFORE_START_TIME_OFFSET..end+HARD_END_TIME_OFFSET){
                //Closest location after trip start
                if (!includedAfterLoc && it.time > end){
                    includedAfterLoc = true
                    trip.add(it)
                }
                //Closest location prior to trip start
                else if (!includedPriorLoc && it.time < start){
                    includedPriorLoc = true
                    trip.add(it)
                }
                //Add all location points during trip and prior loc first if present
                else if (it.time in start..end){
                    trip.add(it)
                }
            }
        }
        return trip
    }
}