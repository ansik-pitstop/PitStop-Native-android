package com.pitstop.interactors.add

import com.pitstop.bluetooth.dataPackages.DtcPackage
import com.pitstop.interactors.Interactor
import com.pitstop.models.Alarm
import com.pitstop.network.RequestError

/**
 * Created by ishan on 2017-10-30.
 */
interface AddAlarmUseCase : Interactor {


    interface Callback{
        fun onAlarmAdded(alarm : Alarm)
        fun onError(requestError: RequestError)
    }

    fun execute(alarm: Alarm, callback: Callback)
}