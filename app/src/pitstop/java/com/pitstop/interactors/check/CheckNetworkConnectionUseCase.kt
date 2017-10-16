package com.pitstop.interactors.check

import com.pitstop.interactors.Interactor

/**
 * Created by Karol Zdebel on 10/16/2017.
 */
interface CheckNetworkConnectionUseCase: Interactor {
    interface Callback{
        fun onGotConnectionStatus(status: Boolean)
    }

    fun execute(callback: Callback)
}