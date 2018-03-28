package com.pitstop.ui.trip

import android.app.PendingIntent
import android.app.Service
import android.content.*
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
import com.pitstop.interactors.add.AddTripDataUseCase
import com.pitstop.interactors.other.EndTripUseCase
import com.pitstop.interactors.other.StartDumpingTripDataWhenConnecteUseCase
import com.pitstop.interactors.other.StartTripUseCase
import com.pitstop.models.DebugMessage
import com.pitstop.models.trip.PendingLocation
import com.pitstop.network.RequestError
import com.pitstop.utils.Logger
import com.pitstop.utils.TimeoutTimer
import com.pitstop.utils.TripUtils

/**
 * Created by Karol Zdebel on 3/1/2018.
 */
class TripsService: Service(), TripActivityObservable, TripParameterSetter, GoogleApiClient.ConnectionCallbacks
        , GoogleApiClient.OnConnectionFailedListener {

    companion object {
        const val LOCATION_UPDATE_INTERVAL = "location_update_interval"
        const val TRIP_START_THRESHOLD = "trip_start_threshold"
        const val LOCATION_UPDATE_PRIORITY = "location_update_priority"
        const val ACTIVITY_UPDATE_INTERVAL = "activity_update_interval"
        const val TRIP_END_THRESHOLD = "trip_end_threshold"
        const val TRIP_TRIGGER = "trip_trigger"
        const val STILL_TIMEOUT = "still_timeout"
        const val TRIP_IN_PROGRESS = "trip_in_progress"
    }

    private val tag = javaClass.simpleName
    private var tripInProgress: Boolean
    private var observers: ArrayList<TripActivityObserver>
    private lateinit var googleApiClient: GoogleApiClient
    private val binder = TripsBinder()
    private lateinit var useCaseComponent: UseCaseComponent
    private var googlePendingIntent: PendingIntent? = null
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var receiver: BroadcastReceiver

    private var locationUpdateInterval = 5000L  //How often location GPS updates are to be received
    private var locationUpdatePriority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY //Battery efficiency vs GPS accuracy
    private var activityUpdateInterval = 3000L  //How often activity updates are received
    private var tripStartThreshold = 70 //Confidence that starts trip
    private var tripEndThreshold = 80   //Confidence that ends trip
    private var tripTrigger = DetectedActivity.IN_VEHICLE   //Activity which triggers trip start
    private var stillTimeoutTime = 600  //Time that a user can remain still in seconds before trip is ended
    private var stillStartConfidence = 90   //Confidence to start still timer
    private var stillEndConfidence = 40 //Confidence to end still timer
    private val locationSizeCache = 2 //How many GPS points are collected in memory before sending to use casse

    private var currentTrip = arrayListOf<Location>()

    private val stillTimeoutTimer = object: TimeoutTimer(stillTimeoutTime/1000,0) {
        override fun onRetry() {
        }

        override fun onTimeout() {
            Logger.getInstance()!!.logI(tag,"Still timer: Timeout",DebugMessage.TYPE_TRIP)
            tripEnd()
        }

    }

    init{
        tripInProgress = false
        observers = arrayListOf()
    }

    inner class TripsBinder : Binder() {
        val service: TripsService
            get() = this@TripsService
    }

    override fun onBind(p0: Intent?): IBinder = binder

    override fun onCreate() {
        Logger.getInstance()!!.logI(tag, "Trips service created", DebugMessage.TYPE_TRIP)

        super.onCreate()

        sharedPreferences = getSharedPreferences(tag, Context.MODE_PRIVATE)

        //Update shared preferences
        locationUpdateInterval = sharedPreferences.getLong(LOCATION_UPDATE_INTERVAL,5000L)
        locationUpdatePriority = sharedPreferences.getInt(LOCATION_UPDATE_PRIORITY
                ,LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
        activityUpdateInterval = sharedPreferences.getLong(ACTIVITY_UPDATE_INTERVAL,3000L)
        tripStartThreshold = sharedPreferences.getInt(TRIP_START_THRESHOLD,70)
        tripEndThreshold = sharedPreferences.getInt(TRIP_END_THRESHOLD,80)
        tripTrigger = sharedPreferences.getInt(TRIP_TRIGGER, DetectedActivity.IN_VEHICLE)
        stillTimeoutTime = sharedPreferences.getInt(STILL_TIMEOUT, 50000)
        tripInProgress = sharedPreferences.getBoolean(TRIP_IN_PROGRESS,false)

        useCaseComponent = DaggerUseCaseComponent.builder()
                .contextModule(ContextModule(applicationContext)).build()

        useCaseComponent.startDumpingTripDataWhenConnectedUseCase
                .execute(object: StartDumpingTripDataWhenConnecteUseCase.Callback{
                    override fun started() {
                        Log.d(tag,"started()")
                    }

                    override fun onError(error: RequestError) {
                        Log.d(tag,"onError() err: $error")
                    }

        })

        val intentFilter = IntentFilter()
        intentFilter.addAction(ActivityService.DETECTED_ACTIVITY)
        intentFilter.addAction(ActivityService.GOT_LOCATION)
        receiver = object : BroadcastReceiver() {
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
        }
        registerReceiver(receiver,intentFilter)

        googleApiClient = GoogleApiClient.Builder(this)
                .addApi(ActivityRecognition.API)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build()

        googleApiClient.connect()
    }

    override fun onDestroy() {
        Logger.getInstance()!!.logI(tag, "Trips service destroyed", DebugMessage.TYPE_TRIP)
        try{
            unregisterReceiver(receiver)
        }catch(e: Exception){
            
        }
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
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

    override fun isTripInProgress(): Boolean = tripInProgress

    override fun setStartThreshold(threshold: Int): Boolean {
        Log.d(tag,"setStartThreshold() threshold: $threshold")
        tripStartThreshold = threshold
        sharedPreferences.edit().putInt(TRIP_START_THRESHOLD,threshold).apply()
        return true
    }

    override fun getStartThreshold(): Int = tripStartThreshold

    override fun setEndThreshold(threshold: Int): Boolean {
        Log.d(tag,"setEndThreshold() threshold: $threshold")
        tripEndThreshold = threshold
        sharedPreferences.edit().putInt(TRIP_END_THRESHOLD,threshold).apply()
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
            sharedPreferences.edit().putLong(LOCATION_UPDATE_INTERVAL,locationUpdateInterval).apply()
            return true
        }
    }

    override fun getLocationUpdateInterval(): Long = locationUpdateInterval

    override fun setLocationUpdatePriority(priority: Int): Boolean {
        Log.d(tag,"setLocationUpdatePriority() priority: $priority")
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
            sharedPreferences.edit().putInt(LOCATION_UPDATE_PRIORITY,locationUpdatePriority).apply()
            return true
        }
    }

    override fun getLocationUpdatePriority(): Int = locationUpdatePriority

    override fun setActivityUpdateInterval(interval: Long): Boolean {
        Log.d(tag,"setActivityUpdateInterval() interval: $interval")
        return if (!googleApiClient.isConnected) false
        else{
            ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates( googleApiClient, googlePendingIntent)
            ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates( googleApiClient, interval, googlePendingIntent)
            activityUpdateInterval = interval
            sharedPreferences.edit().putLong(ACTIVITY_UPDATE_INTERVAL,activityUpdateInterval).apply()
            true
        }
    }

    override fun getActivityUpdateInterval(): Long = activityUpdateInterval

    override fun setActivityTrigger(trigger: Int): Boolean{
        Log.d(tag,"setActivityTrigger() trigger: $trigger")
        return if (!TripUtils.isActivityValid(trigger)) false
        else{
            tripTrigger = trigger
            sharedPreferences.edit().putInt(TRIP_TRIGGER,tripTrigger).apply()
            true
        }
    }

    override fun getActivityTrigger(): Int = tripTrigger

    override fun getStillActivityTimeout(): Int {
        return stillTimeoutTime
    }

    override fun setStillActivityTimeout(timeout: Int) {
        stillTimeoutTime = timeout
        sharedPreferences.edit().putInt(STILL_TIMEOUT,timeout).apply()
    }

    override fun startTripManually(): Boolean {
        Log.d(tag,"startTripManually()")
        return if (tripInProgress) false
        else{
            tripStart()
            return true
        }
    }

    override fun endTripManually(): Boolean {
        Log.d(tag,"endTripManually()")
        return if (tripInProgress){
            tripEnd()
            true
        } else false
    }

    private fun tripStart(){
        Log.d(tag,"tripStart()")
        Logger.getInstance()!!.logI(tag, "Broadcasting trip start", DebugMessage.TYPE_TRIP)
        currentTrip = arrayListOf()
        useCaseComponent.startTripUseCase().execute(object : StartTripUseCase.Callback{
            override fun finished() {
                Log.d(tag,"start trip use case finished()")
                observers.forEach{ it.onTripStart() }
            }
        })
        tripInProgress = true
        sharedPreferences.edit().putBoolean(TRIP_IN_PROGRESS,tripInProgress).apply()

        displayActivityNotif("Trip recording started")
    }

    private fun tripEnd(){
        Log.d(tag,"tripEnd()")
        Logger.getInstance()!!.logI(tag, "Broadcasting trip end", DebugMessage.TYPE_TRIP)
        useCaseComponent.endTripUseCase().execute(currentTrip, object: EndTripUseCase.Callback{
            override fun finished(trip: List<PendingLocation>) {
                Log.d(tag,"end trip use case finished()")
                observers.forEach({ it.onTripEnd(trip) })
            }

            override fun onError(err: RequestError) {
                Log.d(tag,"end trip use case error: ${err.message}")
            }

        })
        currentTrip = arrayListOf()
        tripInProgress = false
        sharedPreferences.edit().putBoolean(TRIP_IN_PROGRESS,tripInProgress).apply()
        displayActivityNotif("Trip recording completed, view app for more info")
    }

    private fun tripUpdate(locations:List<Location>){
        Log.d(tag,"tripUpdate()")
        Logger.getInstance()!!.logI(tag, "Broadcasting trip update: trip = $locations", DebugMessage.TYPE_TRIP)
        currentTrip.addAll(locations)
        //Only launch the use case every 5 location point received, they will be cleared on trip end if leftovers
        if (currentTrip.size > locationSizeCache){
            useCaseComponent.addTripUseCase.execute(currentTrip, object: AddTripDataUseCase.Callback{
                override fun onAddedTripData() {
                    Log.d(tag,"add trip data use case added trip data")
                    observers.forEach{ it.onTripUpdate() }
                }

                override fun onError(err: RequestError) {
                    Log.d(tag,"error adding trip data through use case")
                }
            })
            currentTrip = arrayListOf()
        }
    }

    private fun handleDetectedActivities(probableActivities: List<DetectedActivity>) {
        Log.d(tag, "handleDetectedActivities() tripInProgress: $tripInProgress, probableActivities: $probableActivities")
        for (activity in probableActivities) {

            //skip ignored activities
            if (activity.type == DetectedActivity.TILTING
                    || activity.type == DetectedActivity.UNKNOWN)

            //Start timer if still to end trip on timeout
            else if (activity.type == DetectedActivity.STILL){
                if (tripInProgress && activity.confidence > stillStartConfidence){
                    Logger.getInstance()!!.logI(tag,"Still timer: Started",DebugMessage.TYPE_TRIP)
                    stillTimeoutTimer.start()
                }
            }
            //Trigger trip start, or resume trip from still state
            else if (activity.type == tripTrigger){
                Log.d(tag,"trip trigger received, confidence: "+activity.confidence)
                if (!tripInProgress && activity.confidence > tripStartThreshold){
                    tripStart()
                    stillTimeoutTimer.cancel()
                }else if (tripInProgress && activity.confidence > stillEndConfidence){
                    Logger.getInstance()!!.logI(tag,"Still timer: Cancelled",DebugMessage.TYPE_TRIP)
                    stillTimeoutTimer.cancel()
                }
                break //Don't allow trip end in same receival
                //End trip if type of trigger is NOT ON_FOOT
            }else if (tripTrigger != DetectedActivity.ON_FOOT
                    && activity.type != DetectedActivity.STILL
                    && activity.type != DetectedActivity.UNKNOWN){
                if (tripInProgress && activity.confidence > tripEndThreshold){
                    tripEnd()
                    stillTimeoutTimer.cancel()
                }
            //End trip if type of trigger IS ON_FOOT
            }else if (tripTrigger == DetectedActivity.ON_FOOT
                    && activity.type != DetectedActivity.WALKING && activity.type != DetectedActivity.RUNNING){
                if (tripInProgress && activity.confidence > tripEndThreshold){
                    tripEnd()
                    stillTimeoutTimer.cancel()
                }
            }
            Logger.getInstance()!!.logI(tag, "Activity detected activity" +
                    " = ${TripUtils.activityToString(activity.type)}, confidence = " +
                    "${activity.confidence}", DebugMessage.TYPE_TRIP)
        }
    }

    private fun displayActivityNotif(notif: String){
        val builder = NotificationCompat.Builder(this)
        builder.setContentText(notif)
        builder.setSmallIcon(R.mipmap.ic_launcher)
        builder.setContentTitle(getString(R.string.app_name))
        NotificationManagerCompat.from(this).notify(0, builder.build())
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