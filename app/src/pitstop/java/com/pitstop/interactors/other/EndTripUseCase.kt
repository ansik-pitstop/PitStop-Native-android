package com.pitstop.interactors.other

import android.location.Location
import com.pitstop.interactors.Interactor
import com.pitstop.network.RequestError

/**
 * Created by Karol Zdebel on 3/28/2018.
 */
interface EndTripUseCase: Interactor {
    interface Callback{
        fun finished()
        fun onError(err: RequestError)
    }

    fun execute(trip: List<Location>, callback: Callback)
}