package com.pitstop.interactors.add

import android.location.Location
import android.os.Handler
import com.pitstop.database.LocalTripStorage

/**
 * Created by Karol Zdebel on 3/6/2018.
 */
class AddTripUseCaseImpl(private val localTripStorage: LocalTripStorage
                         , private val useCaseHandler: Handler, private val mainHandler: Handler): AddTripUseCase {

    private lateinit var trip: List<Location>
    private lateinit var callback: AddTripUseCase.Callback

    override fun execute(trip: List<Location>, callback: AddTripUseCase.Callback) {
        this.trip = trip
        this.callback = callback
        useCaseHandler.post(this)
    }

    override fun run() {
        localTripStorage.store(trip)
        mainHandler.post({callback.onAddedTrip()})
    }
}