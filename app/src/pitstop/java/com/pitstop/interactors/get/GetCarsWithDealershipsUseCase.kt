package com.pitstop.interactors.get

import com.pitstop.interactors.Interactor
import com.pitstop.models.Car
import com.pitstop.models.Dealership
import com.pitstop.network.RequestError

/**
 * Created by Karol Zdebel on 10/12/2017.
 */
interface GetCarsWithDealershipsUseCase: Interactor {
    interface Callback{
        fun onGotCarsWithDealerships(data: LinkedHashMap<Car,Dealership>)
        fun onError(error: RequestError)
    }

    fun execute(callback: Callback)
}