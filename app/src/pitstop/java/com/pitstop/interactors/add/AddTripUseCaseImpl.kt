package com.pitstop.interactors.add

import android.location.Geocoder
import android.location.Location
import android.os.Handler
import com.pitstop.database.LocalTripStorage
import com.pitstop.models.DebugMessage
import com.pitstop.network.RequestError
import com.pitstop.utils.Logger
import java.io.IOException

/**
 * Created by Karol Zdebel on 3/6/2018.
 */
class AddTripUseCaseImpl(private val geocoder: Geocoder, private val localTripStorage: LocalTripStorage
                         , private val useCaseHandler: Handler, private val mainHandler: Handler): AddTripUseCase {

    private val TAG = javaClass.simpleName
    private lateinit var trip: List<Location>
    private lateinit var callback: AddTripUseCase.Callback

    override fun execute(trip: List<Location>, callback: AddTripUseCase.Callback) {
        Logger.getInstance().logI(TAG, "Use case execution started, trip: " + trip, DebugMessage.TYPE_USE_CASE)
        this.trip = trip
        this.callback = callback
        useCaseHandler.post(this)
    }

    override fun run() {
        try{
            val startAddress = geocoder
                    .getFromLocation(trip.first().latitude,trip.first().longitude,1).first()
            val endAddress = geocoder
                    .getFromLocation(trip.last().latitude,trip.last().longitude,1).first()


            if (localTripStorage.store(trip) > 0){
                AddTripUseCaseImpl@this.onAddedTrip()
            }else{
                AddTripUseCaseImpl@this.onError(RequestError.getUnknownError())
            }
        }catch(e: IOException){
            e.printStackTrace()
            onError(RequestError.getUnknownError())
        }

    }

    private fun onAddedTrip() {
        Logger.getInstance().logI(TAG, "Use case finished: trip added!", DebugMessage.TYPE_USE_CASE)
        mainHandler.post({callback.onAddedTrip()})
    }

    private fun onError(err: RequestError){
        Logger.getInstance().logE(TAG, "Use case returned error: "+err, DebugMessage.TYPE_USE_CASE)
        mainHandler.post({callback.onError(err)})
    }
}