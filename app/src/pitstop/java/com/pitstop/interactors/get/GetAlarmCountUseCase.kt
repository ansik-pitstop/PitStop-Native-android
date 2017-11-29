package com.pitstop.interactors.get

import com.pitstop.interactors.Interactor
import com.pitstop.network.RequestError

/**
 * Created by ishan on 2017-11-07.
 */
interface GetAlarmCountUseCase : Interactor{

    fun execute(callback: Callback)

    interface Callback{
        fun onAlarmCountGot(alarmCount: Int);
        fun onError(error: RequestError)
    }
}