package com.pitstop.ui.trip

import android.app.IntentService
import android.content.Intent
import android.location.Location
import android.os.Binder
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.util.Log
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.location.LocationResult
import com.pitstop.R


/**
 * Created by Karol Zdebel on 2/27/2018.
 */
class ActivityService: IntentService("ActivityService") {

    companion object {
        val TRIP_START = "trip_start"
        val TRIP_END = "trip_end"
        val TRIP_UPDATE = "trip_update"
        val TRIP_EXTRA = "trip_extra"
    }

    private val tag = javaClass.simpleName
    private val observerList = arrayListOf<TripActivityObserver>()
    private var tripInProgress = false
    private var currentTrip: ArrayList<Location>

    init{
        currentTrip = arrayListOf()
    }

    inner class BluetoothBinder : Binder() {
        val service: ActivityService
            get() = this@ActivityService
    }

    private fun tripStart(){
        tripInProgress = true
        currentTrip = arrayListOf()
        val intent = Intent()
        intent.action = TRIP_START
        sendBroadcast(intent)
    }

    private fun tripEnd(){
        tripInProgress = false
        val intent = Intent()
        intent.action = TRIP_END
        intent.putParcelableArrayListExtra(TRIP_EXTRA,currentTrip)
        currentTrip = arrayListOf()
    }

    private fun tripUpdate(locations:List<Location>){
        currentTrip.addAll(locations)
        val intent = Intent()
        intent.action = TRIP_UPDATE
        intent.putParcelableArrayListExtra(TRIP_EXTRA,currentTrip)
        application.applicationContext
    }

    override fun onHandleIntent(intent: Intent?) {

        if (ActivityRecognitionResult.hasResult(intent)) {
            Log.d(tag,"Got activity intent")
            val activityResult = ActivityRecognitionResult.extractResult(intent)
            handleDetectedActivities(activityResult.probableActivities)
        }
        if (LocationResult.hasResult(intent)){
            Log.d(tag,"Got location intent")
            val locationResult = LocationResult.extractResult(intent)
            handleLocations(locationResult.locations)

        }else{
            Log.d(tag,"location unavailable")
        }
    }

    private fun handleLocations(locations: List<Location>){
        Log.d(tag,"location list: "+locations)
        if (tripInProgress){
            tripUpdate(locations)
        }
    }

    private fun handleDetectedActivities(probableActivities: List<DetectedActivity>) {
        for (activity in probableActivities) {
            when (activity.type) {
                DetectedActivity.ON_FOOT -> {
                    Log.d(tag, "In Vehicle: " + activity.confidence)
                    displayActivityNotif("Vehicle",activity.confidence)
                    if (!tripInProgress && activity.confidence > 80){
                        tripStart()
                    }
                }
                DetectedActivity.ON_BICYCLE -> {
                    Log.d(tag, "On Bicycle: " + activity.confidence)
                    displayActivityNotif("Bicycle",activity.confidence)
                    if (tripInProgress && activity.confidence > 90){
                        tripEnd()
                    }
                }
                DetectedActivity.IN_VEHICLE -> {
                    Log.d(tag, "On Foot: " + activity.confidence)
                    displayActivityNotif("On Foot",activity.confidence)
                    if (tripInProgress && activity.confidence > 90){
                        tripEnd()
                    }
                }
                DetectedActivity.RUNNING -> {
                    Log.d(tag, "Running: " + activity.confidence)
                    displayActivityNotif("Running",activity.confidence)
                    if (tripInProgress && activity.confidence > 90){
                        tripEnd()
                    }
                }
                DetectedActivity.STILL -> {
                    Log.d(tag, "Still: " + activity.confidence)
                    displayActivityNotif("Still",activity.confidence)
                    if (tripInProgress && activity.confidence > 90){
                        tripEnd()
                    }
                }
                DetectedActivity.TILTING -> {
                    Log.d(tag, "Tilting: " + activity.confidence)
                    displayActivityNotif("Tilting",activity.confidence)
                    if (tripInProgress && activity.confidence > 90){
                        tripEnd()
                    }
                }
                DetectedActivity.WALKING -> {
                    Log.d(tag, "Walking: " + activity.confidence)
                    displayActivityNotif("Walking",activity.confidence)
                    if (tripInProgress && activity.confidence > 90){
                        tripEnd()
                    }
                }
                DetectedActivity.UNKNOWN -> {
                    Log.e("ActivityRecogition", "Unknown: " + activity.confidence)
                    displayActivityNotif("Unknown",activity.confidence)
                    if (tripInProgress && activity.confidence > 90){
                        tripEnd()
                    }
                }
            }
        }
    }

    private fun displayActivityNotif(type: String, conf: Int){
        if (conf > 75){
            val builder = NotificationCompat.Builder(this)
            builder.setContentText("$type activity detected with confidence $conf")
            builder.setSmallIcon(R.mipmap.ic_launcher)
            builder.setContentTitle(getString(R.string.app_name))
            NotificationManagerCompat.from(this).notify(0, builder.build())
        }
    }
}