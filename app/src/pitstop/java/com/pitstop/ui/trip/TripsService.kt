package com.pitstop.ui.trip

import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.pitstop.dependency.ContextModule
import com.pitstop.dependency.DaggerUseCaseComponent
import com.pitstop.dependency.UseCaseComponent
import com.pitstop.interactors.other.StartDumpingTripDataWhenConnecteUseCase
import com.pitstop.models.DebugMessage
import com.pitstop.models.trip.CarActivity
import com.pitstop.models.trip.TripState
import com.pitstop.network.RequestError
import com.pitstop.utils.Logger
import io.reactivex.Observable


/**
 * Created by Karol Zdebel on 3/1/2018.
 */
class TripsService: Service(), GoogleApiClient.ConnectionCallbacks
        , GoogleApiClient.OnConnectionFailedListener, TripManualController {

    companion object {
        const val LOC_UPDATE_INTERVAL = 60 * 1000L
        const val LOC_MAX_UPDATE_INTERVAL = 60 * 20 * 1000L
        const val LOC_FASTEST_UPDATE_INTERVAL = 20 * 1000L
        const val ACT_UPDATE_INTERVAL = 30 * 1000L
        const val LOC_UPDATE_PRIORITY = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
    }

    private val tag = javaClass.simpleName
    private lateinit var googleApiClient: GoogleApiClient
    private val binder = TripsBinder()
    private lateinit var useCaseComponent: UseCaseComponent
    private var googlePendingIntent: PendingIntent? = null
    private lateinit var sharedPreferences: SharedPreferences

    inner class TripsBinder : Binder() {
        val service: TripsService
            get() = this@TripsService
    }

    override fun onBind(p0: Intent?): IBinder = binder

    override fun onCreate() {
        Logger.getInstance()!!.logI(tag, "Trips service created", DebugMessage.TYPE_TRIP)

        super.onCreate()

        sharedPreferences = getSharedPreferences(tag, Context.MODE_PRIVATE)

        useCaseComponent = DaggerUseCaseComponent.builder()
                .contextModule(ContextModule(application)).build()

        useCaseComponent.startDumpingTripDataWhenConnectedUseCase
                .execute(object: StartDumpingTripDataWhenConnecteUseCase.Callback{
                    override fun started() {
                        Log.d(tag,"started()")
                    }

                    override fun onError(error: RequestError) {
                        Log.d(tag,"onError() err: $error")
                    }

        })

        val observable: Observable<Boolean> = Observable.create({emitter ->
            val receiver: BroadcastReceiver = object: BroadcastReceiver(){
                override fun onReceive(p0: Context?, p1: Intent?) {
                    if (p1 == null) return
                    Log.d(tag,"received intent, action = ${p1.action}")
                    if (p1.action == TripBroadcastReceiver.ACTION_TRIP_STATE_CHANGE){
                        Log.d(tag,"trip state change = ${p1.getStringExtra(TripBroadcastReceiver.TRIP_RUNNING_STATE)}")
                        when (p1.getStringExtra(TripBroadcastReceiver.TRIP_RUNNING_STATE)){
                            TripBroadcastReceiver.TRIP_RUNNING -> {
                                emitter.onNext(true)
                            }
                            TripBroadcastReceiver.TRIP_NOT_RUNNING -> {
                                emitter.onNext(false)
                            }
                        }
                    }
                }

            }
        })


        googleApiClient = GoogleApiClient.Builder(this)
                .addApi(ActivityRecognition.API)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build()


        googleApiClient.connect()
    }

    override fun getTripState(): Observable<TripState> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onDestroy() {
        Logger.getInstance()!!.logI(tag, "Trips service destroyed", DebugMessage.TYPE_TRIP)
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun startTripManual(){
        Log.d(tag,"startTripManual()")
        val intent = Intent(TripBroadcastReceiver.INTENT_ACTIVITY)
        intent.putExtra(TripBroadcastReceiver.ACTIVITY_TYPE, CarActivity.TYPE_MANUAL_START)
        sendBroadcast(intent)
    }

    override fun endTripManual(){
        Log.d(tag,"endTripManual()")
        val intent = Intent(TripBroadcastReceiver.INTENT_ACTIVITY)
        intent.putExtra(TripBroadcastReceiver.ACTIVITY_TYPE, CarActivity.TYPE_MANUAL_END)
        sendBroadcast(intent)

    }

    private fun beginTrackingLocationUpdates(){
        Log.d(tag,"beginTrackingLocationUpdates()")

        LocationServices.getFusedLocationProviderClient(baseContext).removeLocationUpdates(googlePendingIntent)
        val locationRequest = LocationRequest.create()
        locationRequest.priority = LOC_UPDATE_PRIORITY
        locationRequest.interval = LOC_UPDATE_INTERVAL
        locationRequest.fastestInterval = LOC_FASTEST_UPDATE_INTERVAL
        locationRequest.maxWaitTime = LOC_MAX_UPDATE_INTERVAL

        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(locationRequest)
        val locationSettingsRequest = builder.build()

        // Check whether location settings are satisfied
        val settingsClient = LocationServices.getSettingsClient(this)
        settingsClient.checkLocationSettings(locationSettingsRequest)

        try{
             LocationServices.getFusedLocationProviderClient(baseContext)
                    .requestLocationUpdates(locationRequest,googlePendingIntent)
        }catch(e: SecurityException){
            e.printStackTrace()
        }
    }

    override fun onConnected(p0: Bundle?) {
        Log.d(tag,"onConnected() google api")
        Logger.getInstance()!!.logI(tag, "Google API connected", DebugMessage.TYPE_TRIP)

        val intent = Intent(this, TripBroadcastReceiver::class.java)
        intent.action = TripBroadcastReceiver.ACTION_PROCESS_UPDATE
        googlePendingIntent = PendingIntent.getBroadcast( this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT )

        ActivityRecognition.getClient(baseContext)
                .requestActivityUpdates(ACT_UPDATE_INTERVAL, googlePendingIntent)

        beginTrackingLocationUpdates()
        stopSelf()
    }

    override fun onConnectionSuspended(p0: Int) {
        Log.d(tag,"onConnectionSuspended() google api")
        Logger.getInstance()!!.logE(tag, "Google API connection suspended", DebugMessage.TYPE_TRIP)
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        Logger.getInstance()!!.logE(tag, "Google API connection failed", DebugMessage.TYPE_TRIP)
    }

}