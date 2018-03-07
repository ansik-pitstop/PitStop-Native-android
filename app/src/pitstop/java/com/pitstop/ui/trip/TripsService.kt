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
import com.pitstop.dependency.ContextModule
import com.pitstop.dependency.DaggerUseCaseComponent
import com.pitstop.dependency.UseCaseComponent
import com.pitstop.interactors.add.AddTripUseCase
import com.pitstop.models.DebugMessage
import com.pitstop.network.RequestError
import com.pitstop.utils.Logger
import com.pitstop.utils.TripUtils

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
    private var activityUpdateInterval = 3000L
    private var tripStartThreshold = 70
    private var tripEndThreshold = 30
    private var tripTrigger = DetectedActivity.ON_FOOT

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
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient,googlePendingIntent)
            val locationRequest = LocationRequest()
            locationRequest.interval = interval
            locationRequest.priority = locationUpdatePriority
            try{
                LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, googlePendingIntent)
            }catch(e: SecurityException){
                e.printStackTrace()
                return false
            }
            locationUpdateInterval = interval
            return true
        }
    }

    override fun getLocationUpdateInterval(): Long = locationUpdateInterval

    override fun setLocationUpdatePriority(priority: Int): Boolean {
        if (!googleApiClient.isConnected) return false
        else{
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient,googlePendingIntent)
            val locationRequest = LocationRequest()
            locationRequest.interval = locationUpdateInterval
            locationRequest.priority = priority
            try{
                LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, googlePendingIntent)
            }catch(e: SecurityException){
                e.printStackTrace()
                return false
            }
            locationUpdatePriority = priority
            return true
        }
    }

    override fun getLocationUpdatePriority(): Int = locationUpdatePriority

    override fun setActivityUpdateInterval(interval: Long): Boolean {
        return if (!googleApiClient.isConnected) false
        else{
            ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates( googleApiClient, googlePendingIntent)
            ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates( googleApiClient, interval, googlePendingIntent)
            activityUpdateInterval = interval
            true
        }
    }

    override fun getActivityUpdateInterval(): Long = activityUpdateInterval

    override fun setActivityTrigger(trigger: Int): Boolean{
        return if (!TripUtils.isActivityValid(trigger)) false
        else{
            tripTrigger = trigger
            true
        }
    }

    override fun getActivityTrigger(): Int = tripTrigger


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
            displayActivityNotif(TripUtils.activityToString(activity.type), activity.confidence)
            if (activity.type == tripTrigger){
                if (!tripInProgress && activity.confidence > tripStartThreshold){
                    tripStart()
                }
            }else{
                if (tripInProgress && activity.confidence > tripEndThreshold){
                    tripEnd()
                }
            }
            Logger.getInstance()!!.logI(tag, "Activity detected activity" +
                    " = ${TripUtils.activityToString(activity.type)}, confidence = " +
                    "${activity.confidence}", DebugMessage.TYPE_TRIP)
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
        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates( googleApiClient, activityUpdateInterval, googlePendingIntent)

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