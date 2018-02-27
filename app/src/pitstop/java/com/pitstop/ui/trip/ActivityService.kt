package com.pitstop.ui.trip

import android.app.IntentService
import android.content.Intent
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.util.Log
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity
import com.pitstop.R




/**
 * Created by Karol Zdebel on 2/27/2018.
 */
class ActivityService: IntentService("ActivityService") {

    val tag = javaClass.simpleName

    override fun onHandleIntent(intent: Intent?) {
        if (ActivityRecognitionResult.hasResult(intent)) {
            val result = ActivityRecognitionResult.extractResult(intent)
            handleDetectedActivities(result.probableActivities)
        }
    }

    private fun handleDetectedActivities(probableActivities: List<DetectedActivity>) {
        for (activity in probableActivities) {
            when (activity.type) {
                DetectedActivity.IN_VEHICLE -> {
                    Log.d(tag, "In Vehicle: " + activity.confidence)
                    displayActivityNotif("Vehicle",activity.confidence)
                }
                DetectedActivity.ON_BICYCLE -> {
                    Log.d(tag, "On Bicycle: " + activity.confidence)
                    displayActivityNotif("Bicycle",activity.confidence)
                }
                DetectedActivity.ON_FOOT -> {
                    Log.d(tag, "On Foot: " + activity.confidence)
                    displayActivityNotif("On Foot",activity.confidence)
                }
                DetectedActivity.RUNNING -> {
                    Log.d(tag, "Running: " + activity.confidence)
                    displayActivityNotif("Running",activity.confidence)
                }
                DetectedActivity.STILL -> {
                    Log.d(tag, "Still: " + activity.confidence)
                    displayActivityNotif("Still",activity.confidence)
                }
                DetectedActivity.TILTING -> {
                    Log.d(tag, "Tilting: " + activity.confidence)
                    displayActivityNotif("Tilting",activity.confidence)
                }
                DetectedActivity.WALKING -> {
                    Log.d(tag, "Walking: " + activity.confidence)
                    displayActivityNotif("Walking",activity.confidence)
                }
                DetectedActivity.UNKNOWN -> {
                    Log.e("ActivityRecogition", "Unknown: " + activity.confidence)
                    displayActivityNotif("Unknown",activity.confidence)
                }
            }
        }
    }

    private fun displayActivityNotif(type: String, conf: Int){
        val builder = NotificationCompat.Builder(this)
        builder.setContentText("$type activity detected with confidence $conf")
        builder.setSmallIcon(R.mipmap.ic_launcher)
        builder.setContentTitle(getString(R.string.app_name))
        NotificationManagerCompat.from(this).notify(0, builder.build())
    }
}