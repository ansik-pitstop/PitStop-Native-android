package com.pitstop.ui.trip

import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.os.Bundle
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.*
import com.pitstop.R
import com.pitstop.ui.main_activity.MainActivity

/**
 * Created by Karol Zdebel on 3/1/2018.
 */
class TripsService: Service(), TripActivityObservable, GoogleApiClient.ConnectionCallbacks
        , GoogleApiClient.OnConnectionFailedListener {

    private val tag = javaClass.simpleName
    private var tripInProgress: Boolean
    private var currentTrip: ArrayList<Location>
    private var observers: ArrayList<TripActivityObserver>
    private lateinit var googleApiClient: GoogleApiClient


    private final val TRIP_START_THRESHHOLD = 90
    private final val TRIP_END_THRESHHOLD = 30

    init{
        tripInProgress = false
        currentTrip = arrayListOf()
        observers = arrayListOf()
    }

    override fun onBind(p0: Intent?): IBinder {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onCreate() {
        super.onCreate()

        val intentFilter = IntentFilter()
        intentFilter.addAction(ActivityService.DETECTED_ACTIVITY)

        applicationContext.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, intent: Intent?) {
                if (ActivityRecognitionResult.hasResult(intent)) {
                    Log.d(tag,"Got activity intent")
                    val activityResult = ActivityRecognitionResult.extractResult(intent)
                    handleDetectedActivities(activityResult.probableActivities)
                }
                if (LocationResult.hasResult(intent)){
                    Log.d(tag,"Got location intent")
                    val locationResult = LocationResult.extractResult(intent)
                    if (tripInProgress && locationResult != null)
                        tripUpdate(locationResult.locations)

                }else{
                    Log.d(tag,"location unavailable")
                }
            }
        },intentFilter)

        googleApiClient = GoogleApiClient.Builder(this)
                .addApi(ActivityRecognition.API)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build()

        googleApiClient.connect()
    }

    override fun subscribeTripActivity(observer: TripActivityObserver) {
        if (!observers.contains(observer)){
            observers.add(observer)
        }
    }

    override fun unsubscribeTripActivity(observer: TripActivityObserver) {
        observers.remove(observer)
    }

    override fun getCurrentTrip(): List<Location> = currentTrip

    override fun isTripInProgress(): Boolean = tripInProgress


    private fun tripStart(){
        Log.d(tag,"tripStart()")
        tripInProgress = true
        currentTrip = arrayListOf()
        for (o in observers){
            o.onTripStart()
        }
    }

    private fun tripEnd(){
        Log.d(tag,"tripEnd()")
        tripInProgress = false
        for (o in observers){
            o.onTripEnd(currentTrip)
        }
        currentTrip = arrayListOf()
    }

    private fun tripUpdate(locations:List<Location>){
        Log.d(tag,"tripUpdate()")
        currentTrip.addAll(locations)
        for (o in observers){
            o.onTripUpdate(currentTrip)
        }
    }


    private fun handleDetectedActivities(probableActivities: List<DetectedActivity>) {
        Log.d(tag, "handleDetectedActivities() tripInProgress: " + tripInProgress)
        for (activity in probableActivities) {
            when (activity.type) {
                DetectedActivity.ON_FOOT -> {
                    Log.d(tag, "In Vehicle: " + activity.confidence)
                    displayActivityNotif("Vehicle", activity.confidence)
                    if (!tripInProgress && activity.confidence > TRIP_START_THRESHHOLD){
                        tripStart()
                    }

                }
                DetectedActivity.ON_BICYCLE -> {
                    Log.d(tag, "On Bicycle: " + activity.confidence)
                    displayActivityNotif("Bicycle", activity.confidence)
                    if (tripInProgress && activity.confidence > TRIP_END_THRESHHOLD){
                        tripEnd()
                    }

                }
                DetectedActivity.IN_VEHICLE -> {
                    Log.d(tag, "On Foot: " + activity.confidence)
                    displayActivityNotif("On Foot", activity.confidence)
                    if (tripInProgress && activity.confidence > TRIP_END_THRESHHOLD){
                        tripEnd()
                    }

                }
                DetectedActivity.RUNNING -> {
                    Log.d(tag, "Running: " + activity.confidence)
                    displayActivityNotif("Running", activity.confidence)
                    if (!tripInProgress && activity.confidence > TRIP_START_THRESHHOLD){
                        tripStart()
                    }
                }
                DetectedActivity.STILL -> {
                    Log.d(tag, "Still: " + activity.confidence)
                    displayActivityNotif("Still", activity.confidence)
                    if (tripInProgress && activity.confidence > TRIP_END_THRESHHOLD){
                        tripEnd()
                    }

                }
                DetectedActivity.TILTING -> {
                    Log.d(tag, "Tilting: " + activity.confidence)
                }
                DetectedActivity.WALKING -> {
                    Log.d(tag, "Walking: " + activity.confidence)
                    displayActivityNotif("Walking", activity.confidence)
                    if (!tripInProgress && activity.confidence > TRIP_START_THRESHHOLD){
                        tripStart()
                    }

                }
                DetectedActivity.UNKNOWN -> {
                    Log.e("ActivityRecogition", "Unknown: " + activity.confidence)
                    displayActivityNotif("Unknown", activity.confidence)
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

    override fun onConnected(p0: Bundle?) {
        Log.d(MainActivity.TAG,"onConnected() google api")
        val intent = Intent(this, ActivityService::class.java)
        val pendingIntent = PendingIntent.getService( this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT )
        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates( googleApiClient, 1000, pendingIntent)

        val locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        try{
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, pendingIntent)
        }catch(e: SecurityException){
            e.printStackTrace()
        }
    }

    override fun onConnectionSuspended(p0: Int) {
        Log.d(MainActivity.TAG,"onConnectionSuspended() google api")
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        Log.d(MainActivity.TAG,"onConnectionFailed() google api")
    }

}