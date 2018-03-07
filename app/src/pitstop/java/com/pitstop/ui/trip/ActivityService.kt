package com.pitstop.ui.trip

import android.app.IntentService
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.LocationResult


/**
 * Created by Karol Zdebel on 2/27/2018.
 */
class ActivityService: IntentService("ActivityService") {

    private val tag = javaClass.simpleName

    companion object {
        val DETECTED_ACTIVITY = "detected_activity"
        val GOT_LOCATION = "got_location"
        val ACTIVITY_EXTRA = "activity_extra"
        val LOCATION_EXTRA = "location_extra"
    }

    override fun onHandleIntent(intent: Intent?) {
        Log.d(javaClass.simpleName,"onHandleIntent() intent: "+intent?.action)

        if (ActivityRecognitionResult.hasResult(intent)) {
            val activityResult = ActivityRecognitionResult.extractResult(intent)
            Log.d(tag,"activity result probably activities: "+activityResult.probableActivities)
            val intent = Intent(DETECTED_ACTIVITY)
            intent.putExtra(ACTIVITY_EXTRA,activityResult)
            sendBroadcast(intent)
        }
        if (LocationResult.hasResult(intent)){
            val locationResult = LocationResult.extractResult(intent)
            Log.d(tag,"location result locations: "+locationResult.locations)
            val intent = Intent(GOT_LOCATION)
            intent.putExtra(LOCATION_EXTRA,locationResult)
            sendBroadcast(intent)
        }else{
            Log.d(tag,"location unavailable")
        }
    }
}