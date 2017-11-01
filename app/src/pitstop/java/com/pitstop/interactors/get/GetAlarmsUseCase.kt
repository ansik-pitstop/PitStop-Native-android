package com.pitstop.interactors.get

import android.widget.ArrayAdapter
import com.pitstop.interactors.Interactor
import com.pitstop.models.Alarm
import com.pitstop.network.RequestError

/**
 * Created by ishan on 2017-10-30.
 */

interface GetAlarmsUseCase: Interactor{

    fun execute(carID: Int, callbakc: Callback)

    interface Callback{
        fun onAlarmsGot(alarms: HashMap<String, ArrayList<Alarm>>);
        fun onError(error: RequestError)
    }
}
