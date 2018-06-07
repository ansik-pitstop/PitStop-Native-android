package com.pitstop.ui.trip

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.location.LocationResult
import com.pitstop.database.LocalActivityStorage
import com.pitstop.database.LocalCarStorage
import com.pitstop.database.LocalLocationStorage
import com.pitstop.database.LocalUserStorage
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
        const val DETECTED_ACTIVITY = "detected_activity"
        const val GOT_LOCATION = "got_location"
        const val ACTIVITY_EXTRA = "activity_extra"
        const val LOCATION_EXTRA = "location_extra"
        const val ACTION_PROCESS_UPDATE = "action_process_update"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(tag,"onReceive()")
        var still = false
        if (LocationResult.hasResult(intent)){
            val locationResult = LocationResult.extractResult(intent)
            val intent = Intent(TripBroadcastReceiver.GOT_LOCATION)
            intent.putExtra(LOCATION_EXTRA,locationResult)
            Logger.getInstance().logD(tag,"Received activity location intent, loc.size = " +
                    "${locationResult.locations.size}", DebugMessage.TYPE_TRIP)
            //sendBroadcast(intent)

            Log.d(tag,"received locations: ${locationResult.locations}")

            val localUserStorage = LocalUserStorage(context)
            if (localUserStorage.user == null || localUserStorage.user.id == -1) return
            val carId = localUserStorage.user.settings.carId
            val localCarStorage = LocalCarStorage(context)
            val vin = localCarStorage.getCar(carId)?.vin
            val localLocationStorage = LocalLocationStorage(context)
            val locations = arrayListOf<CarLocation>()
            Log.d(tag,"got vin: $vin")
            locationResult.locations.forEach({
                locations.add(CarLocation(vin ?: "",System.currentTimeMillis(),it.longitude,it.latitude))
            })
            val lastLocalLoc = localLocationStorage.getAll().lastOrNull()
            Log.d(tag,"last loc: $lastLocalLoc, locations received: ${locations[0]}")
            if (lastLocalLoc != null && lastLocalLoc.latitude == locations[0].latitude && lastLocalLoc.longitude == locations[0].longitude){
                still = true
            }
            val rows = localLocationStorage.store(locations)
            Logger.getInstance().logD(tag,"Stored locations locally, response: $rows"
                    , DebugMessage.TYPE_TRIP)
        }else{
            Log.d(tag,"location unavailable")
        }

        val detectedActivities = arrayListOf<DetectedActivity>()
        Log.d(tag,"still=$still")
        if (still){

            detectedActivities.add(DetectedActivity(DetectedActivity.IN_VEHICLE,10))
            detectedActivities.add(DetectedActivity(DetectedActivity.ON_FOOT,95))
            detectedActivities.add(DetectedActivity(DetectedActivity.STILL,10))
            detectedActivities.add(DetectedActivity(DetectedActivity.WALKING,95))


        }else{
            detectedActivities.add(DetectedActivity(DetectedActivity.IN_VEHICLE,80))
            detectedActivities.add(DetectedActivity(DetectedActivity.ON_FOOT,20))
            detectedActivities.add(DetectedActivity(DetectedActivity.STILL,10))
            detectedActivities.add(DetectedActivity(DetectedActivity.WALKING,20))

        }

        val localUserStorage = LocalUserStorage(context)
        val carId = localUserStorage.user.settings.carId
        val localCarStorage = LocalCarStorage(context)
        if (localUserStorage.user == null || localUserStorage.user.id == -1) return
        val vin = localCarStorage.getCar(carId)?.vin
        val localActivityStorage = LocalActivityStorage(context)
        val carActivity = arrayListOf<CarActivity>()
//            activityResult.probableActivities.forEach({
//                carActivity.add(CarActivity(vin ?: "",System.currentTimeMillis(),it.type,it.confidence))
//            })

        detectedActivities.forEach({
            carActivity.add(CarActivity(vin ?: "",System.currentTimeMillis(),it.type,it.confidence))
        })
        Log.d(tag,"got vin: $vin")
        val rows = localActivityStorage.store(carActivity)
        Logger.getInstance().logD(tag,"Stored detected activities locally, response: $rows"
                , DebugMessage.TYPE_TRIP)

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