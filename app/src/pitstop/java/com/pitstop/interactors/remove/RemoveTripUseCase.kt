package com.pitstop.interactors.remove

import com.pitstop.interactors.Interactor
import com.pitstop.network.RequestError

/**
 * Created by David C. on 26/3/18.
 */
interface RemoveTripUseCase : Interactor {

    interface Callback {

        fun onTripRemoved()
        fun onCantRemoveTrip()
        fun onError(error: RequestError)

    }

    fun execute(tripId: String, vin: String, callback: Callback)

}