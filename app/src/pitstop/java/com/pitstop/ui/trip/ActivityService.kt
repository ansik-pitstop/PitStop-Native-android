package com.pitstop.ui.trip

import android.content.Intent
import android.support.v4.app.JobIntentService
import android.util.Log
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.LocationResult
import com.pitstop.models.DebugMessage
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
            sendBroadcast(intent)
        }
        if (LocationResult.hasResult(intent)){
            val locationResult = LocationResult.extractResult(intent)
            val intent = Intent(GOT_LOCATION)
            intent.putExtra(LOCATION_EXTRA,locationResult)
            Logger.getInstance().logD(tag,"Received activity location intent, loc.size = " +
                    "${locationResult.locations.size}",DebugMessage.TYPE_TRIP)
            sendBroadcast(intent)
        }else{
            Log.d(tag,"location unavailable")
        }
    }

}