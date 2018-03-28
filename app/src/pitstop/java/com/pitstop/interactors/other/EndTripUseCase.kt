package com.pitstop.interactors.other

import android.location.Location
import com.pitstop.interactors.Interactor

/**
 * Created by Karol Zdebel on 3/28/2018.
 */
interface EndTripUseCase: Interactor {
    interface Callback{
        fun finished(trip: List<Location>)
    }

    fun execute(callback: Callback)
}