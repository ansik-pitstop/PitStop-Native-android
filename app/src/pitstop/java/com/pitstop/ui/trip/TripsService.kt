package com.pitstop.ui.trip

import android.app.PendingIntent
import android.app.Service
import android.content.*
import android.location.Location
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.*
import com.pitstop.dependency.ContextModule
import com.pitstop.dependency.DaggerUseCaseComponent
import com.pitstop.dependency.UseCaseComponent
import com.pitstop.interactors.add.AddTripDataUseCase
import com.pitstop.interactors.other.EndTripUseCase
import com.pitstop.interactors.other.StartDumpingTripDataWhenConnecteUseCase
import com.pitstop.interactors.other.StartTripUseCase
import com.pitstop.models.DebugMessage
import com.pitstop.models.trip.RecordedLocation
import com.pitstop.network.RequestError
import com.pitstop.utils.Logger
import com.pitstop.utils.NotificationsHelper
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
        const val STILL_TIMEOUT = "still_timeout"
        const val TRIP_IN_PROGRESS = "trip_in_progress"
        const val MINIMUM_LOCATION_ACCURACY = "mnimum_location_accuracy"
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
    private val locationSizeCache = 5 //How many GPS points are collected in memory before sending to use casse
    private var minLocationAccuracy = 60 //Minimum location accuracy required for a GPS point to not be discarded
    private var stillTimerRunning = false //Whether timer is ticking
    private var stillTimeoutTimer: TimeoutTimer
    private var currentTrip = arrayListOf<RecordedLocation>()
    private var recentConfidence = 0

    init{
        tripInProgress = false
        observers = arrayListOf()
        this.stillTimeoutTimer = getStillTimeoutTimer(60000)
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
        tripInProgress = sharedPreferences.getBoolean(TRIP_IN_PROGRESS,false)
        minLocationAccuracy = sharedPreferences.getInt(MINIMUM_LOCATION_ACCURACY,minLocationAccuracy)

        Logger.getInstance().logI(tag,"Trip settings: {locInterval" +
                "=$locationUpdateInterval, locPriority=$locationUpdatePriority" +
                ", actInterval=$activityUpdateInterval, startThresh=$tripStartThreshold" +
                ", tripEndThresh=$tripEndThreshold" +
                ", tripProg=$tripInProgress, timerRun=$stillTimerRunning, minAcc=$minLocationAccuracy}"
                ,DebugMessage.TYPE_TRIP)

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
        cancelStillTimer()
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
            beginTrackingLocationUpdates(interval,locationUpdatePriority)
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
            beginTrackingLocationUpdates(locationUpdateInterval,priority)
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

    override fun getStillActivityTimeout(): Int {
        Log.d(tag,"getStillActivityTimeout()")
        return sharedPreferences.getInt(STILL_TIMEOUT, 60000)
    }

    override fun setStillActivityTimeout(timeout: Int) {
        Log.d(tag,"setStillActivityTimeout() timeout: $timeout")
        stillTimeoutTimer = getStillTimeoutTimer(timeout)
        sharedPreferences.edit().putInt(STILL_TIMEOUT,timeout).apply()
    }

    override fun getMinimumLocationAccuracy(): Int {
        Log.d(tag,"getMinimumLocationAccuracy() acc: $minLocationAccuracy")

        return minLocationAccuracy
    }

    override fun setMinimumLocationAccuracy(acc: Int) {
        Log.d(tag,"setMinimumLocationAccuracy() acc: $acc")
        sharedPreferences.edit().putInt(MINIMUM_LOCATION_ACCURACY,acc).apply()
        minLocationAccuracy = acc
    }

    override fun startTripManually(): Boolean {
        Logger.getInstance().logI(tag,"Attempting to start trip manually" +
                ", tripInProgress = $tripInProgress",DebugMessage.TYPE_TRIP)
        return if (tripInProgress) false
        else{
            tripStart()
            return true
        }
    }

    override fun endTripManually(): Boolean {
        Logger.getInstance().logI(tag,"Attempting to end trip manually" +
                ", tripInProgress = $tripInProgress",DebugMessage.TYPE_TRIP)
        return if (tripInProgress){
            tripEnd()
            true
        } else false
    }

    private fun cancelStillTimer(){
        Logger.getInstance()!!.logI(tag,"Still timer: Cancelled",DebugMessage.TYPE_TRIP)
        stillTimeoutTimer.cancel()
        stillTimerRunning = false
    }

    private fun startStillTimer(){
        Logger.getInstance()!!.logI(tag,"Still timer: Started",DebugMessage.TYPE_TRIP)
        stillTimeoutTimer.start()
        stillTimerRunning = true
    }

    private fun tripStart(){
        Log.d(tag,"tripStart()")
        Logger.getInstance()!!.logI(tag, "Broadcasting trip start", DebugMessage.TYPE_TRIP)
        cancelStillTimer()
        currentTrip = arrayListOf()
        useCaseComponent.startTripUseCase().execute(object : StartTripUseCase.Callback{
            override fun finished() {
                Log.d(tag,"start trip use case finished()")
                observers.forEach{ it.onTripStart() }
            }
        })
        tripInProgress = true
        sharedPreferences.edit().putBoolean(TRIP_IN_PROGRESS,tripInProgress).apply()
        beginTrackingLocationUpdates(locationUpdateInterval,locationUpdatePriority)
        startForeground(NotificationsHelper.TRIPS_FG_NOTIF_ID
                ,NotificationsHelper.getForegroundTripServiceNotification(baseContext))
    }

    private fun tripEnd(){
        Log.d(tag,"tripEnd()")
        Logger.getInstance()!!.logI(tag, "Broadcasting trip end", DebugMessage.TYPE_TRIP)
        cancelStillTimer()
        useCaseComponent.endTripUseCase().execute(currentTrip, object: EndTripUseCase.Callback{
            override fun tripDiscarded() {
                Log.w(tag,"end trip use case discarded trip!")
                NotificationsHelper.sendNotification(applicationContext,"Trip discarded due to low vehicle activity"
                        ,"Pitstop")
            }

            override fun finished() {
                Log.d(tag,"end trip use case finished()")
                observers.forEach({ it.onTripEnd() })
                NotificationsHelper.sendNotification(applicationContext,"Trip recording completed"
                        ,"Pitstop")
            }

            override fun onError(err: RequestError) {
                Log.d(tag,"end trip use case error: ${err.message}")
            }

        })
        currentTrip = arrayListOf()
        tripInProgress = false
        sharedPreferences.edit().putBoolean(TRIP_IN_PROGRESS,tripInProgress).apply()
        stopTrackingLocationUpdates()
        stopForeground(true)
    }

    private fun tripUpdate(locations:List<Location>){
        Log.d(tag,"tripUpdate()")
        Logger.getInstance()!!.logI(tag, "Broadcasting trip update: trip = " +
                "${locations.filter { it.accuracy < minLocationAccuracy }}", DebugMessage.TYPE_TRIP)

        //Filter locations based on minimum accuracy
        currentTrip.addAll(locations
                .filter{ it.accuracy < minLocationAccuracy }
                .map { RecordedLocation(it.time,it.longitude,it.latitude,recentConfidence)})

        //Only launch the use case every 5 location point received, they will be cleared on trip end if leftovers
        if (currentTrip.size > locationSizeCache){
            useCaseComponent.addTripDataUseCase.execute(currentTrip, object: AddTripDataUseCase.Callback{
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

        var onFootActivity: DetectedActivity? = null
        var vehicleActivty: DetectedActivity? = null
        var stillActivity: DetectedActivity? = null

        probableActivities.forEach({ activity ->
            Logger.getInstance()!!.logD(tag, "Activity detected activity" +
                    " = ${TripUtils.activityToString(activity.type)}, confidence = " +
                    "${activity.confidence}", DebugMessage.TYPE_TRIP)
            when(activity.type){
                DetectedActivity.IN_VEHICLE -> {
                    recentConfidence = activity.confidence
                    vehicleActivty = activity
                }
                DetectedActivity.ON_FOOT -> onFootActivity = activity
                DetectedActivity.STILL -> stillActivity = activity
            }
        })

        //Start trip if possibly driving and definitely not walking
        if (!tripInProgress && vehicleActivty != null && vehicleActivty!!.confidence > 30
                && ( onFootActivity == null || onFootActivity!!.confidence < 40)) {
            tripStart()
        //End trip if definitely walking
        }else if  (tripInProgress && onFootActivity != null && onFootActivity!!.confidence > 95){
            tripEnd()
        //Start still timer if definitely not driving, and definitely still or walking
        }else if (!stillTimerRunning && ( ( stillActivity != null && stillActivity!!.confidence == 100)
                || ( onFootActivity != null && onFootActivity!!.confidence > 80))
                && (vehicleActivty == null || vehicleActivty!!.confidence < 30)) {
            startStillTimer()
        //Cancel still timer if likely driving and not definitely walking
        }else if (stillTimerRunning && vehicleActivty != null && vehicleActivty!!.confidence > 30
                && (onFootActivity == null || onFootActivity!!.confidence < 70)){
            cancelStillTimer()
        }
    }

    private fun getStillTimeoutTimer(timeoutTime: Int): TimeoutTimer {
        return object : TimeoutTimer(timeoutTime / 1000, 0) {
            override fun onRetry() {
            }

            override fun onTimeout() {
                Logger.getInstance()!!.logI(tag, "Still timer: Timeout, tripInProgress=$tripInProgress"
                        , DebugMessage.TYPE_TRIP)
                stillTimerRunning = false
                tripEnd()
            }
        }
    }

    private fun beginTrackingLocationUpdates(interval: Long, priority: Int): Boolean{
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient,googlePendingIntent)
        val locationRequest = LocationRequest()
        locationRequest.interval = interval
        locationRequest.priority = priority
        try{
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, googlePendingIntent)
        }catch(e: SecurityException){
            e.printStackTrace()
            return false
        }
        return true
    }

    private fun stopTrackingLocationUpdates(){
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient,googlePendingIntent)
    }

    override fun onConnected(p0: Bundle?) {
        Log.d(tag,"onConnected() google api")
        Logger.getInstance()!!.logI(tag, "Google API connected", DebugMessage.TYPE_TRIP)
        val intent = Intent(this, ActivityService::class.java)
        googlePendingIntent = PendingIntent.getService( this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT )
        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates( googleApiClient, activityUpdateInterval, googlePendingIntent)

        if (tripInProgress)
            beginTrackingLocationUpdates(locationUpdateInterval,locationUpdatePriority)
    }

    override fun onConnectionSuspended(p0: Int) {
        Log.d(tag,"onConnectionSuspended() google api")
        Logger.getInstance()!!.logE(tag, "Google API connection suspended", DebugMessage.TYPE_TRIP)
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        Logger.getInstance()!!.logE(tag, "Google API connection failed", DebugMessage.TYPE_TRIP)
    }

}