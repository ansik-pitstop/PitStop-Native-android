package com.pitstop.ui.trip

import android.content.Intent
import android.support.v4.app.JobIntentService
import android.util.Log
import com.google.android.gms.location.ActivityRecognitionResult
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
 * Created by Karol Zdebel on 2/27/2018.
 */
class ActivityService: JobIntentService() {

    private val tag = javaClass.simpleName

    companion object {
        val DETECTED_ACTIVITY = "detected_activity"
        val GOT_LOCATION = "got_location"
        val ACTIVITY_EXTRA = "activity_extra"
        val LOCATION_EXTRA = "location_extra"
    }

    override fun onHandleWork(intent: Intent) {
        Log.d(javaClass.simpleName,"onHandleIntent() intent: "+intent?.action)

        if (ActivityRecognitionResult.hasResult(intent)) {
            val activityResult = ActivityRecognitionResult.extractResult(intent)
            Logger.getInstance().logD(tag,"Received activity recognition intent",DebugMessage.TYPE_TRIP)
            val intent = Intent(DETECTED_ACTIVITY)
            intent.putExtra(ACTIVITY_EXTRA,activityResult)
            //sendBroadcast(intent)

            val localUserStorage = LocalUserStorage(baseContext)
            val carId = localUserStorage.user.settings.carId
            if (localUserStorage.user == null || localUserStorage.user.id == -1) return
            val localCarStorage = LocalCarStorage(baseContext)
            val vin = localCarStorage.getCar(carId)?.vin
            val localActivityStorage = LocalActivityStorage(baseContext)
            val carActivity = arrayListOf<CarActivity>()
            activityResult.probableActivities.forEach({
                carActivity.add(CarActivity(vin ?: "",System.currentTimeMillis(),it.type,it.confidence))
            })
            val rows = localActivityStorage.store(carActivity)
            Log.d(tag,"Stored detected activities locally, response: $rows")
        }
        if (LocationResult.hasResult(intent)){
            val locationResult = LocationResult.extractResult(intent)
            val intent = Intent(GOT_LOCATION)
            intent.putExtra(LOCATION_EXTRA,locationResult)
            Logger.getInstance().logD(tag,"Received activity location intent, loc.size = " +
                    "${locationResult.locations.size}",DebugMessage.TYPE_TRIP)
            //sendBroadcast(intent)

            val localUserStorage = LocalUserStorage(baseContext)
            if (localUserStorage.user == null || localUserStorage.user.id == -1) return
            val carId = localUserStorage.user.settings.carId
            val localCarStorage = LocalCarStorage(baseContext)
            val vin = localCarStorage.getCar(carId)?.vin
            val localLocationStorage = LocalLocationStorage(baseContext)
            val locations = arrayListOf<CarLocation>()
            locationResult.locations.forEach({
                locations.add(CarLocation(vin ?: "",System.currentTimeMillis(),it.longitude,it.latitude))
            })
            val rows = localLocationStorage.store(locations)
            Log.d(tag,"Stored locations locally, response: $rows")
        }else{
            Log.d(tag,"location unavailable")
        }

    }

}