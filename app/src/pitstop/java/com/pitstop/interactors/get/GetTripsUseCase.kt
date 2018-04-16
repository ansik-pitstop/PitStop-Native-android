package com.pitstop.interactors.get

import com.pitstop.interactors.Interactor
import com.pitstop.models.trip.Trip
import com.pitstop.network.RequestError

/**
 * Created by David C. on 12/3/18.
 */
interface GetTripsUseCase : Interactor {

    interface Callback {

        fun onTripsRetrieved(tripList: List<Trip>, isLocal: Boolean)
        fun onNoCar()
        fun onError(error: RequestError)

    }

    //Execute the use case
    fun execute(callback: Callback)

}