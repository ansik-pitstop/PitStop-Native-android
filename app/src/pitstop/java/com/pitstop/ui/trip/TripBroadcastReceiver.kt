package com.pitstop.ui.trip

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.model.LatLng
import com.pitstop.database.*
import com.pitstop.dependency.ContextModule
import com.pitstop.dependency.DaggerUseCaseComponent
import com.pitstop.interactors.other.ProcessTripDataUseCase
import com.pitstop.models.DebugMessage
import com.pitstop.models.trip.CarActivity
import com.pitstop.models.trip.CarLocation
import com.pitstop.models.trip.TripState
import com.pitstop.models.trip.TripStateType
import com.pitstop.utils.Logger
import com.pitstop.utils.NotificationsHelper
import com.pitstop.utils.TripUtils

/**
 * Created by Karol Zdebel on 6/7/2018.
 */
class TripBroadcastReceiver: BroadcastReceiver() {

    private val tag = TripBroadcastReceiver::class.java.simpleName

    companion object {
        const val GOT_LOCATION = "got_location"
        const val LOCATION_EXTRA = "location_extra"
        const val ACTION_PROCESS_UPDATE = "action_process_update"
        const val MIN_LOC_ACCURACY = 100
        const val TIME_CURRENT_STATE = "current_state_time"
        const val TYPE_CURRENT_STATE = "current_state_type"
        const val READY_TO_PROCESS_TRIP_DATA = "process_trip_data" //Wait for locations before processing trip data since they can be delayed
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(tag,"onReceive()")

        val currentTime = System.currentTimeMillis()
        val sharedPreferences = context.getSharedPreferences(tag, Context.MODE_PRIVATE)

        if (ActivityRecognitionResult.hasResult(intent)) {
            val activityResult = ActivityRecognitionResult.extractResult(intent)
            Logger.getInstance().logD(tag,"Received activity recognition intent",DebugMessage.TYPE_TRIP)

            val localUserStorage = LocalUserStorage(LocalDatabaseHelper.getInstance(context))
            if (localUserStorage.user == null || localUserStorage.user.id == -1){
                Logger.getInstance().logE(tag,"User id is null!",DebugMessage.TYPE_TRIP)
                return
            }
            val carId = localUserStorage.user.settings.carId
            val localCarStorage = LocalCarStorage(LocalDatabaseHelper.getInstance(context))
            val vin = localCarStorage.getCar(carId)?.vin
            val localActivityStorage = LocalActivityStorage(LocalDatabaseHelper.getInstance(context))
            val carActivity = arrayListOf<CarActivity>()

            if (vin == null){
                Logger.getInstance().logE(tag,"Vin is null! ",DebugMessage.TYPE_TRIP)
            }

            var onFootActivity = 0
            var stillActivity = 0
            var vehicleActivity = 0

            activityResult.probableActivities.forEach({

                if (it.type == DetectedActivity.ON_FOOT){
                    onFootActivity = it.confidence
                }else if (it.type == DetectedActivity.STILL){
                    stillActivity = it.confidence
                }else if (it.type == DetectedActivity.IN_VEHICLE){
                    vehicleActivity = it.confidence
                }
                carActivity.add(CarActivity(vin ?: "", currentTime,it.type,it.confidence))
            })

            val currentStateType = sharedPreferences.getInt(TYPE_CURRENT_STATE, TripStateType.TRIP_NONE.value)
            val currentStateTime = sharedPreferences.getLong(TIME_CURRENT_STATE, System.currentTimeMillis())
            val currentTripState = TripState(TripStateType.values().first { it.value == currentStateType },currentStateTime)

            //Move this code to use case
            val nextState = TripUtils.getNextTripState(currentTripState,carActivity)

            if (currentTripState != nextState){
                val notifMessage = when (nextState.tripStateType){
                    TripStateType.TRIP_DRIVING_HARD -> "Trip driving hard"
                    TripStateType.TRIP_DRIVING_SOFT -> "Trip driving soft"
                    TripStateType.TRIP_STILL -> "Trip still"
                    TripStateType.TRIP_END_SOFT ->{
                        sharedPreferences.edit().putBoolean(READY_TO_PROCESS_TRIP_DATA,true).apply()
                        "Trip soft end"
                    }
                    TripStateType.TRIP_END_HARD -> {
                        //Allow for processing trip data on end, but wait for next location bundle to come in
                        sharedPreferences.edit().putBoolean(READY_TO_PROCESS_TRIP_DATA,true).apply()
                        "Trip hard end"
                    }
                    TripStateType.TRIP_NONE -> "Trip none"
                }
                NotificationsHelper.sendNotification(context,notifMessage,"Pitstop")
            }

            Logger.getInstance().logD(tag,"Important activities: {on foot: $onFootActivity" +
                    ", still: $stillActivity, vehicle: $vehicleActivity}",DebugMessage.TYPE_TRIP)


            val rows = localActivityStorage.store(carActivity)
            Logger.getInstance().logD(tag,"Stored activities locally, response: $rows"
                    , DebugMessage.TYPE_TRIP)
        }
        if (LocationResult.hasResult(intent)){
            val locationResult = LocationResult.extractResult(intent)
            val intent = Intent(TripBroadcastReceiver.GOT_LOCATION)
            intent.putExtra(LOCATION_EXTRA,locationResult)

            Logger.getInstance().logD(tag,"Received activity location intent, loc.size = " +
                    "${locationResult.locations.size}", DebugMessage.TYPE_TRIP)

            val accuracies = arrayListOf<Float>()
            val coords = locationResult.locations.onEach { accuracies.add(it.accuracy) }
                    .map { LatLng(it.latitude,it.longitude) }
            Logger.getInstance().logD(tag,"received locations: $coords, accuracies: $accuracies"
                    ,DebugMessage.TYPE_TRIP)

            val localUserStorage = LocalUserStorage(LocalDatabaseHelper.getInstance(context))
            if (localUserStorage.user == null || localUserStorage.user.id == -1){
                Logger.getInstance().logE(tag,"User id is null!",DebugMessage.TYPE_TRIP)
                return
            }
            val carId = localUserStorage.user.settings.carId
            val localCarStorage = LocalCarStorage(LocalDatabaseHelper.getInstance(context))
            val vin = localCarStorage.getCar(carId)?.vin
            val localLocationStorage = LocalLocationStorage(LocalDatabaseHelper.getInstance(context))
            val locations = arrayListOf<CarLocation>()

            if (vin == null){
                Logger.getInstance().logE(tag,"Vin is null!",DebugMessage.TYPE_TRIP)
            }

            locationResult.locations.filter { it.accuracy < MIN_LOC_ACCURACY }.forEach({
                locations.add(CarLocation(vin ?: "",currentTime,it.longitude,it.latitude))
            })

            val rows = localLocationStorage.store(locations)
            Logger.getInstance().logD(tag,"Stored locations locally, response: $rows"
                    , DebugMessage.TYPE_TRIP)

            if (sharedPreferences.getBoolean(READY_TO_PROCESS_TRIP_DATA,false)){
                val useCaseComponent = DaggerUseCaseComponent.builder()
                        .contextModule(ContextModule(context)).build()

                useCaseComponent.processTripDataUseCase().execute(object: ProcessTripDataUseCase.Callback{
                    override fun processed(trip: List<List<CarLocation>>) {
                        Log.d(tag,"processed() trip: $trip")
                    }
                })
                sharedPreferences.edit().putBoolean(READY_TO_PROCESS_TRIP_DATA,false).apply()
            }


        }

    }

//******************** Code used for generating fake activity during location change*********************
//    val detectedActivities = arrayListOf<DetectedActivity>()
//    Log.d(tag,"still=$still")
//    if (still){
//
//        detectedActivities.add(DetectedActivity(DetectedActivity.IN_VEHICLE,10))
//        detectedActivities.add(DetectedActivity(DetectedActivity.ON_FOOT,95))
//        detectedActivities.add(DetectedActivity(DetectedActivity.STILL,10))
//        detectedActivities.add(DetectedActivity(DetectedActivity.WALKING,95))
//
//
//    }else{
//        detectedActivities.add(DetectedActivity(DetectedActivity.IN_VEHICLE,80))
//        detectedActivities.add(DetectedActivity(DetectedActivity.ON_FOOT,20))
//        detectedActivities.add(DetectedActivity(DetectedActivity.STILL,10))
//        detectedActivities.add(DetectedActivity(DetectedActivity.WALKING,20))
//
//    }
//
//    val localUserStorage = LocalUserStorage(context)
//    val carId = localUserStorage.user.settings.carId
//    val localCarStorage = LocalCarStorage(context)
//    if (localUserStorage.user == null || localUserStorage.user.id == -1) return
//    val vin = localCarStorage.getCar(carId)?.vin
//    val localActivityStorage = LocalActivityStorage(context)
//    val carActivity = arrayListOf<CarActivity>()
////            activityResult.probableActivities.forEach({
////                carActivity.add(CarActivity(vin ?: "",System.currentTimeMillis(),it.type,it.confidence))
////            })
//
//    detectedActivities.forEach({
//        carActivity.add(CarActivity(vin ?: "",System.currentTimeMillis(),it.type,it.confidence))
//    })
//    Log.d(tag,"got vin: $vin")
//    val rows = localActivityStorage.store(carActivity)
//    Logger.getInstance().logD(tag,"Stored detected activities locally, response: $rows"
//    , DebugMessage.TYPE_TRIP)

}