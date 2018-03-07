package com.pitstop.ui.trip

import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.*
import com.pitstop.R
import com.pitstop.R.string.threshold
import com.pitstop.dependency.ContextModule
import com.pitstop.dependency.DaggerUseCaseComponent
import com.pitstop.dependency.UseCaseComponent
import com.pitstop.interactors.add.AddTripUseCase
import com.pitstop.models.DebugMessage
import com.pitstop.network.RequestError
import com.pitstop.utils.Logger

/**
 * Created by Karol Zdebel on 3/1/2018.
 */
class TripsService: Service(), TripActivityObservable, TripParameterSetter, GoogleApiClient.ConnectionCallbacks
        , GoogleApiClient.OnConnectionFailedListener {

    private val tag = javaClass.simpleName
    private var tripInProgress: Boolean
    private var currentTrip: ArrayList<Location>
    private var observers: ArrayList<TripActivityObserver>
    private lateinit var googleApiClient: GoogleApiClient
    private val binder = TripsBinder()
    private lateinit var useCaseComponent: UseCaseComponent
    private var googlePendingIntent: PendingIntent? = null

    private var locationUpdateInterval = 5000L
    private var locationUpdatePriority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
    private var tripStartThreshold = 70
    private var tripEndThreshold = 30

    init{
        tripInProgress = false
        currentTrip = arrayListOf()
        observers = arrayListOf()
    }

    inner class TripsBinder : Binder() {
        val service: TripsService
            get() = this@TripsService
    }

    override fun onBind(p0: Intent?): IBinder = binder

    override fun onCreate() {
        Log.d(tag,"onCreate()")
        super.onCreate()

        useCaseComponent = DaggerUseCaseComponent.builder()
                .contextModule(ContextModule(applicationContext)).build()

        val intentFilter = IntentFilter()
        intentFilter.addAction(ActivityService.DETECTED_ACTIVITY)
        intentFilter.addAction(ActivityService.GOT_LOCATION)
        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, intent: Intent?) {
                Log.d(tag,"onReceive() intent: "+intent)
                if (intent?.action == ActivityService.DETECTED_ACTIVITY){
                    Log.d(tag,"Received detected activity intent")
                    val result = intent?.getParcelableExtra<ActivityRecognitionResult>(
                            ActivityService.ACTIVITY_EXTRA)
                    handleDetectedActivities(result.probableActivities)
                }else if (intent?.action == ActivityService.GOT_LOCATION){
                    Log.d(tag,"Received location intent")
                    val result = intent?.getParcelableExtra<LocationResult>(
                            ActivityService.LOCATION_EXTRA)
                    if (tripInProgress){
                        tripUpdate(result.locations)
                    }
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
        Logger.getInstance()!!.logI(tag, "Observer subscribed", DebugMessage.TYPE_TRIP)
        if (!observers.contains(observer)){
            observers.add(observer)
        }
    }

    override fun unsubscribeTripActivity(observer: TripActivityObserver) {
        Logger.getInstance()!!.logI(tag, "Observer unsubscribed", DebugMessage.TYPE_TRIP)
        observers.remove(observer)
    }

    override fun getCurrentTrip(): List<Location> = currentTrip

    override fun isTripInProgress(): Boolean = tripInProgress

    override fun setStartThreshold(threshold: Int): Boolean {
        Log.d(tag,"setStartThreshold() threshold: $threshold")
        tripStartThreshold = threshold
        return true
    }

    override fun getStartThreshold(): Int = tripStartThreshold

    override fun setEndThreshold(threshold: Int): Boolean {
        Log.d(tag,"setEndThreshold() threshold: $threshold")
        tripEndThreshold = threshold
        return true
    }

    override fun getEndThreshold(): Int = tripEndThreshold

    override fun setLocationUpdateInterval(interval: Long): Boolean {
        Log.d(tag,"setLocationUpdateInterval() interval: $interval")
        if (!googleApiClient.isConnected || interval < 1000L) return false
        else{
            locationUpdateInterval = interval
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient,googlePendingIntent)
            val locationRequest = LocationRequest()
            locationRequest.interval = interval
            locationRequest.priority = locationUpdatePriority
            try{
                LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, googlePendingIntent)
            }catch(e: SecurityException){
                e.printStackTrace()
            }
        }
    }

    override fun getLocationUpdateInterval(): Long = locationUpdateInterval

    override fun setLocationUpdatePriority(priority: Int): Boolean {
        if (!googleApiClient.isConnected) return false
        else{

        }
    }

    override fun getLocationUpdatePriority(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setActivityUpdateInterval(interval: Int): Boolean {
        if (!googleApiClient.isConnected) return false
        else{

        }
    }

    override fun getActivityUpdateInterval(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setActivityTrigger(trigger: Int){

    }

    override fun getActivityTrigger(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    private fun tripStart(){
        Log.d(tag,"tripStart()")
        Logger.getInstance()!!.logI(tag, "Broadcasting trip start", DebugMessage.TYPE_TRIP)
        tripInProgress = true
        currentTrip = arrayListOf()
        for (o in observers){
            o.onTripStart()
        }
    }

    private fun tripEnd(){
        Log.d(tag,"tripEnd()")
        Logger.getInstance()!!.logI(tag, "Broadcasting trip end", DebugMessage.TYPE_TRIP)
        tripInProgress = false
        for (o in observers){
            o.onTripEnd(currentTrip)
        }
        useCaseComponent.addTripUseCase.execute(currentTrip, object: AddTripUseCase.Callback{
            override fun onAddedTrip() {
            }

            override fun onError(err: RequestError) {
            }
        })
        currentTrip = arrayListOf()
    }

    private fun tripUpdate(locations:List<Location>){
        Log.d(tag,"tripUpdate()")
        Logger.getInstance()!!.logI(tag, "Broadcasting trip update: trip = $locations", DebugMessage.TYPE_TRIP)
        currentTrip.addAll(locations)
        for (o in observers){
            o.onTripUpdate(currentTrip)
        }
    }

    private fun handleDetectedActivities(probableActivities: List<DetectedActivity>) {
        Log.d(tag, "handleDetectedActivities() tripInProgress: $tripInProgress, probableActivities: $probableActivities")
        for (activity in probableActivities) {
            var activityType = ""
            when (activity.type) {
                DetectedActivity.ON_FOOT -> {
                    Log.d(tag, "In Vehicle: " + activity.confidence)
                    displayActivityNotif("Vehicle", activity.confidence)
                    if (!tripInProgress && activity.confidence > tripStartThreshold){
                        tripStart()
                    }
                    activityType = "ON_FOOT"

                }
                DetectedActivity.ON_BICYCLE -> {
                    Log.d(tag, "On Bicycle: " + activity.confidence)
                    displayActivityNotif("Bicycle", activity.confidence)
                    if (tripInProgress && activity.confidence > tripEndThreshold){
                        tripEnd()
                    }
                    activityType = "ON_BICYCLE"
                }
                DetectedActivity.IN_VEHICLE -> {
                    Log.d(tag, "On Foot: " + activity.confidence)
                    displayActivityNotif("On Foot", activity.confidence)
                    if (tripInProgress && activity.confidence > tripEndThreshold){
                        tripEnd()
                    }
                    activityType = "IN_VEHICLE"
                }
                DetectedActivity.RUNNING -> {
                    Log.d(tag, "Running: " + activity.confidence)
                    displayActivityNotif("Running", activity.confidence)
                    if (!tripInProgress && activity.confidence > tripStartThreshold){
                        tripStart()
                    }
                    activityType = "RUNNING"
                }
                DetectedActivity.STILL -> {
                    Log.d(tag, "Still: " + activity.confidence)
                    displayActivityNotif("Still", activity.confidence)
                    if (tripInProgress && activity.confidence > tripEndThreshold){
                        tripEnd()
                    }
                    activityType = "STILL"
                }
                DetectedActivity.TILTING -> {
                    Log.d(tag, "Tilting: " + activity.confidence)
                    activityType = "TILTING"
                }
                DetectedActivity.WALKING -> {
                    Log.d(tag, "Walking: " + activity.confidence)
                    displayActivityNotif("Walking", activity.confidence)
                    if (!tripInProgress && activity.confidence > tripStartThreshold){
                        tripStart()
                    }
                    activityType = "WALKING"
                }
                DetectedActivity.UNKNOWN -> {
                    Log.e("ActivityRecogition", "Unknown: " + activity.confidence)
                    displayActivityNotif("Unknown", activity.confidence)
                    activityType = "UNKNOWN"
                }
            }
            Logger.getInstance()!!.logI(tag, "Activity detected activity = $activityType" +
                    ", confidence = ${activity.confidence}", DebugMessage.TYPE_TRIP)
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
        Log.d(tag,"onConnected() google api")
        Logger.getInstance()!!.logI(tag, "Google API connected", DebugMessage.TYPE_TRIP)
        val intent = Intent(this, ActivityService::class.java)
        googlePendingIntent = PendingIntent.getService( this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT )
        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates( googleApiClient, 1000, googlePendingIntent)

        val locationRequest = LocationRequest()
        locationRequest.priority = locationUpdatePriority
        locationRequest.interval = locationUpdateInterval
        try{
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, googlePendingIntent)
        }catch(e: SecurityException){
            e.printStackTrace()
        }
    }

    override fun onConnectionSuspended(p0: Int) {
        Log.d(tag,"onConnectionSuspended() google api")
        Logger.getInstance()!!.logI(tag, "Google API connection suspended", DebugMessage.TYPE_TRIP)
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        Log.d(tag,"onConnectionFailed() google api")
        Logger.getInstance()!!.logI(tag, "Google API connection failed", DebugMessage.TYPE_TRIP)

    }

}