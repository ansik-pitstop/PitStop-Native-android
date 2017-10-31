package com.pitstop.interactors.get

import com.pitstop.interactors.Interactor
import com.pitstop.models.Alarm
import com.pitstop.network.RequestError

/**
 * Created by ishan on 2017-10-30.
 */

interface GetAlarmsUseCase: Interactor{

    fun execute(carID: Int, callbakc: Callback)

    interface Callback{
        fun onAlarmsGot(alarmList: MutableList<Alarm>);
        fun onError(error: RequestError)
    }
}
