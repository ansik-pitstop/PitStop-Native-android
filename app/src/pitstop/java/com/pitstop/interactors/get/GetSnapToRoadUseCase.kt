package com.pitstop.interactors.get

import com.pitstop.interactors.Interactor
import com.pitstop.models.snapToRoad.SnappedPoint
import com.pitstop.network.RequestError

/**
 * Created by David C. on 16/3/18.
 */
interface GetSnapToRoadUseCase : Interactor {

    interface Callback {

        fun onSnapToRoadRetrieved(snappedPointList: List<SnappedPoint>)
        fun onError(error: RequestError)

    }

    //Execute the use case
    fun execute(listLatLng: String, interpolate: String, callback: Callback)

}