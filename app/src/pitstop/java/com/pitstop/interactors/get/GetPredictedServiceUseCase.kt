package com.pitstop.interactors.get

import com.pitstop.interactors.Interactor
import com.pitstop.network.RequestError
import com.pitstop.retrofit.PredictedService

/**
 * Created by Karol Zdebel on 2/6/2018.
 */
interface GetPredictedServiceUseCase: Interactor {
    interface Callback{
        fun onGotPredictedService(predictedService: PredictedService)
        fun onError(error: RequestError)
    }

    fun execute(callback: Callback)
}