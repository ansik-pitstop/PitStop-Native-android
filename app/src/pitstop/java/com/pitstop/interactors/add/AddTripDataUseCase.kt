package com.pitstop.interactors.add

import com.pitstop.interactors.Interactor
import com.pitstop.models.trip.RecordedLocation
import com.pitstop.network.RequestError

/**
 * Created by Karol Zdebel on 3/6/2018.
 */
interface AddTripDataUseCase : Interactor {
    interface Callback{
        fun onAddedTripData()
        fun onError(err: RequestError)
    }

    fun execute(trip: List<RecordedLocation>, callback: Callback)
}