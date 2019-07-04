package com.pitstop.ui.trip

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Handler
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
        const val INTENT_ACTIVITY = "com.pitstop.ui.trip.TripBroadcastReceiver.intent_activity"
        const val ACTIVITY_TYPE = "activity_type"
        const val ACTIVITY_TIME = "activity_time"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(tag,"onReceive()")

        val currentTime = System.currentTimeMillis()
        val sharedPreferences = context.getSharedPreferences(tag, Context.MODE_PRIVATE)

        val localLocationStorage = LocalLocationStorage(LocalDatabaseHelper.getInstance(context))
        val localActivityStorage = LocalActivityStorage(LocalDatabaseHelper.getInstance(context))
        val localUserStorage = LocalUserStorage(LocalDatabaseHelper.getInstance(context))
        if (localUserStorage.user == null || localUserStorage.user.id == -1){
            Logger.getInstance().logE(tag,"User id is null!",DebugMessage.TYPE_TRIP)
            return
        }
        val carId = localUserStorage.user.settings.carId
        val localCarStorage = LocalCarStorage(LocalDatabaseHelper.getInstance(context))
        val car = localCarStorage.getCar(carId)

        if (car?.scannerId?.contains("danlaw") == true) {
            return
        }

        val vin = car?.vin ?: ""
        if (vin.isEmpty()){
            Logger.getInstance().logE(tag,"Vin is null! ",DebugMessage.TYPE_TRIP)
        }

        val carActivity = arrayListOf<CarActivity>()
-
        Log.d(tag,"got intent action: "+intent.action)

        if (intent.action == INTENT_ACTIVITY){
            val activityType = intent.getIntExtra(ACTIVITY_TYPE,CarActivity.TYPE_OTHER)
            val time = intent.getLongExtra(ACTIVITY_TIME,System.currentTimeMillis())
            Log.d(tag,"Got INTENT_ACTIVITY, type: $activityType, has activity type? ${intent.hasExtra(ACTIVITY_TYPE)}")
            carActivity.add(CarActivity(vin = vin, conf = 100
                    , type = activityType, time = time))
        }

        if (ActivityRecognitionResult.hasResult(intent)) {
            val activityResult = ActivityRecognitionResult.extractResult(intent)
            Logger.getInstance().logD(tag,"Received activity recognition intent",DebugMessage.TYPE_TRIP)

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
                carActivity.add(CarActivity(vin ?: "", currentTime
                        ,TripUtils.getCarActivityType(it),it.confidence))
            })

            Logger.getInstance().logD(tag,"Important activities: {on foot: $onFootActivity" +
                    ", still: $stillActivity, vehicle: $vehicleActivity}",DebugMessage.TYPE_TRIP)

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

        //Store car activity both from manual start & end, and detected activity API
        if (!carActivity.isEmpty()){

            val currentTripState = getCurrentTripState(sharedPreferences)

            val nextState = TripUtils.getNextTripState(currentTripState,carActivity)

            //Broadcast next state to TripsService so state can be displayed in UI
            val nextStateIntent = Intent()
            nextStateIntent.action = TYPE_CURRENT_STATE
            nextStateIntent.putExtra(TYPE_CURRENT_STATE,nextState.tripStateType.value)
            context.sendBroadcast(nextStateIntent)

            Logger.getInstance().logD(tag, "current state: $currentTripState" +
                    ", next state: $nextState",DebugMessage.TYPE_TRIP)

            if (currentTripState != nextState){
                //Launch still timer and end trip if 10 min goes by and the still state hasn't changed
                if (nextState.tripStateType == TripStateType.TRIP_STILL_HARD) {
                    Logger.getInstance().logD(tag, "Still timer started",DebugMessage.TYPE_TRIP)
                    Handler().postDelayed({
                        Logger.getInstance().logD(tag, "Still timer ended current trip state ${getCurrentTripState(sharedPreferences)}",DebugMessage.TYPE_TRIP)
                        if (getCurrentTripState(sharedPreferences) == nextState) {
                            Logger.getInstance().logD(tag, "Still timer timeout",DebugMessage.TYPE_TRIP)
                            localActivityStorage.store(arrayListOf(CarActivity(vin ?: ""
                                    ,System.currentTimeMillis()+1000,CarActivity.TYPE_STILL_TIMEOUT,100)))
                            sharedPreferences.edit().putBoolean(READY_TO_PROCESS_TRIP_DATA,true).apply()
                            val nextStateAfterStill = TripState(TripStateType.TRIP_NONE, System.currentTimeMillis())
                            setCurrentState(sharedPreferences,nextStateAfterStill)

                            val nextIntent = Intent()
                            nextIntent.action = TYPE_CURRENT_STATE
                            nextIntent.putExtra(TYPE_CURRENT_STATE,nextStateAfterStill.tripStateType.value)
                            context.sendBroadcast(nextIntent)

//                            NotificationsHelper.sendNotification(context
//                                    ,"Trip finished recording, it may take a moment to appear in the app","Pitstop")
                        }

                    },TripUtils.STILL_TIMEOUT.toLong())
                }

//                val notifMessage = when (nextState.tripStateType){
//                    TripStateType.TRIP_DRIVING_HARD -> "Trip recording"
//                    TripStateType.TRIP_END_SOFT ->{
//                        sharedPreferences.edit().putBoolean(READY_TO_PROCESS_TRIP_DATA,true).apply()
//                        "Trip finished recording, it may take a moment to appear in the app"
//                    }
//                    TripStateType.TRIP_END_HARD -> {
//                        //Allow for processing trip data on end, but wait for next location bundle to come in
//                        sharedPreferences.edit().putBoolean(READY_TO_PROCESS_TRIP_DATA,true).apply()
//                        "Trip finished recording, it may take a moment to appear in the app"
//                    }
//                    TripStateType.TRIP_MANUAL_END -> {
//                        sharedPreferences.edit().putBoolean(READY_TO_PROCESS_TRIP_DATA,true).apply()
//                        null
//                    }
//                    else -> null
//                }
//                if (notifMessage != null)
//                    NotificationsHelper.sendNotification(context,notifMessage,"Pitstop")
            }

            setCurrentState(sharedPreferences,nextState)

            val rows = localActivityStorage.store(carActivity)
            Logger.getInstance().logD(tag,"Stored activities locally, response: $rows"
                    , DebugMessage.TYPE_TRIP)
        }

    }

    private fun setCurrentState(sharedPreferences: SharedPreferences, nextState: TripState){
        sharedPreferences.edit().putInt(TYPE_CURRENT_STATE,nextState.tripStateType.value).apply()
        sharedPreferences.edit().putLong(TIME_CURRENT_STATE,nextState.time).apply()
    }

    private fun getCurrentTripState(sharedPreferences: SharedPreferences): TripState {
        val currentStateType = sharedPreferences.getInt(TYPE_CURRENT_STATE, TripStateType.TRIP_NONE.value)
        val currentStateTime = sharedPreferences.getLong(TIME_CURRENT_STATE, System.currentTimeMillis())
        return TripState(TripStateType.values().first { it.value == currentStateType },currentStateTime)
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