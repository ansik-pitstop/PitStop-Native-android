package com.pitstop.interactors.set

import com.pitstop.interactors.Interactor
import com.pitstop.network.RequestError

/**
 * Created by ishan on 2017-11-02.
 */
interface SetAlarmsEnabledUseCase: Interactor {

    interface Callback {
        fun onAlarmsEnabledSet()
        fun onError(error: RequestError)
    }

    fun execute(alarmsEnabled: Boolean, callback: Callback)
}