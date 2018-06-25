package com.pitstop.ui.trip

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.location.LocationResult
import com.pitstop.database.*
import com.pitstop.models.DebugMessage
import com.pitstop.models.trip.CarActivity
import com.pitstop.models.trip.CarLocation
import com.pitstop.utils.Logger

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
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(tag,"onReceive()")
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
                carActivity.add(CarActivity(vin ?: "",System.currentTimeMillis(),it.type,it.confidence))
            })

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

            Log.d(tag,"received locations: ${locationResult.locations}")

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
                locations.add(CarLocation(vin ?: "",System.currentTimeMillis(),it.longitude,it.latitude))
            })

            val rows = localLocationStorage.store(locations)
            Logger.getInstance().logD(tag,"Stored locations locally, response: $rows"
                    , DebugMessage.TYPE_TRIP)
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