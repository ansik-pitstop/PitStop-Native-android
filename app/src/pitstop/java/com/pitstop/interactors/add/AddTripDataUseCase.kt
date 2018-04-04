package com.pitstop.interactors.add

import android.location.Location
import com.pitstop.interactors.Interactor
import com.pitstop.network.RequestError

/**
 * Created by Karol Zdebel on 3/6/2018.
 */
interface AddTripDataUseCase : Interactor {
    interface Callback{
        fun onAddedTripData()
        fun onError(err: RequestError)
    }

    fun execute(trip: List<Location>, callback: Callback)
}