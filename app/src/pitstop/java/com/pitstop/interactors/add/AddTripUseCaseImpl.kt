package com.pitstop.interactors.add

import android.location.Location
import android.os.Handler
import com.pitstop.database.LocalTripStorage
import com.pitstop.models.DebugMessage
import com.pitstop.network.RequestError
import com.pitstop.utils.Logger

/**
 * Created by Karol Zdebel on 3/6/2018.
 */
class AddTripUseCaseImpl(private val localTripStorage: LocalTripStorage
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
        localTripStorage.store(trip)
        AddTripUseCaseImpl@this.onAddedTrip()
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