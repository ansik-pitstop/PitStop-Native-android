package com.pitstop.interactors.other

import com.pitstop.interactors.Interactor
import com.pitstop.models.trip.RecordedLocation
import com.pitstop.network.RequestError

/**
 * Created by Karol Zdebel on 3/28/2018.
 */
interface EndTripUseCase: Interactor {
    interface Callback{
        fun tripDiscarded()
        fun finished()
        fun onError(err: RequestError)
    }

    fun execute(trip: List<RecordedLocation>, callback: Callback)
}