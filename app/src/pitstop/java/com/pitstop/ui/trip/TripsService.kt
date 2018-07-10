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
import com.pitstop.network.RequestError
import com.pitstop.utils.Logger



/**
 * Created by Karol Zdebel on 3/1/2018.
 */
class TripsService: Service(), GoogleApiClient.ConnectionCallbacks
        , GoogleApiClient.OnConnectionFailedListener {

    companion object {
        const val LOC_UPDATE_INTERVAL = 10000L
        const val LOC_MAX_UPDATE_INTERVAL = 1200000L
        const val LOC_FASTEST_UPDATE_INTERVAL = 10000L
        const val ACT_UPDATE_INTERVAL = 3000L
        const val LOC_UPDATE_PRIORITY = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        const val PROCESS_TRIPS_INTERVAL = 1800000L //30 min
    }

    private val tag = javaClass.simpleName
    private lateinit var googleApiClient: GoogleApiClient
    private val binder = TripsBinder()
    private lateinit var useCaseComponent: UseCaseComponent
    private var googlePendingIntent: PendingIntent? = null
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var receiver: BroadcastReceiver
    private var tripsProcessing = false

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
    }

    override fun onConnectionSuspended(p0: Int) {
        Log.d(tag,"onConnectionSuspended() google api")
        Logger.getInstance()!!.logE(tag, "Google API connection suspended", DebugMessage.TYPE_TRIP)
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        Logger.getInstance()!!.logE(tag, "Google API connection failed", DebugMessage.TYPE_TRIP)
    }

}