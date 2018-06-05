package com.pitstop.interactors.other

import com.pitstop.interactors.Interactor
import com.pitstop.models.trip.CarLocation

/**
 * Created by Karol Zdebel on 6/4/2018.
 */
interface ProcessTripDataUseCase: Interactor {

    interface Callback{
        fun processed(trip: List<List<CarLocation>>)
    }

    fun execute(callback: Callback)
}