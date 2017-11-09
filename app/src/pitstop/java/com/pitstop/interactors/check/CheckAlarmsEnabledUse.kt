package com.pitstop.interactors.check

import com.pitstop.interactors.Interactor
import com.pitstop.network.RequestError

/**
 * Created by ishan on 2017-11-02.
 */
interface CheckAlarmsEnabledUse:Interactor {
    interface Callback {
        fun onAlarmsEnabledChecked(alarmsEnabled: Boolean)
        fun onError(error: RequestError)
    }

    fun execute(callback: Callback)
}
