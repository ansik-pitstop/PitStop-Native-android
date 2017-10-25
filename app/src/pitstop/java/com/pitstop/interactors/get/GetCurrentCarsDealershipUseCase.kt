package com.pitstop.interactors.get

import com.pitstop.interactors.Interactor
import com.pitstop.models.Dealership
import com.pitstop.network.RequestError

/**
 * Created by Karol Zdebel on 10/25/2017.
 */
interface GetCurrentCarsDealershipUseCase: Interactor {
    interface Callback{
        fun onGotDealership(dealership: Dealership)
        fun onNoCarExists()
        fun onError(error: RequestError)
    }

    fun execute(callback: Callback)
}