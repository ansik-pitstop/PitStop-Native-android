package com.pitstop.interactors.other

import com.pitstop.interactors.Interactor
import com.pitstop.network.RequestError

interface SendFleetManagerSmsUseCase: Interactor {
    interface Callback {
        fun onSuccess()
        fun onError(err: RequestError)
    }

    fun execute(text: String, callback: SendFleetManagerSmsUseCase.Callback)
}