package com.pitstop.interactors.get

import com.pitstop.interactors.Interactor
import com.pitstop.models.Alarm
import com.pitstop.network.RequestError

/**
 * Created by ishan on 2017-11-07.
 */
interface GetAlarmCountUseCase : Interactor{

    fun execute(carID:Int, callback: Callback)

    interface Callback{
        fun onAlarmCountGot(alarmCount: Int);
        fun onError(error: RequestError)
    }
}