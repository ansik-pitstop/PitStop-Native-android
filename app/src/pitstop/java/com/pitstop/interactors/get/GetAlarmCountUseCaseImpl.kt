package com.pitstop.interactors.get

import android.os.Handler
import android.util.Log
import com.pitstop.database.LocalAlarmStorage
import com.pitstop.models.Alarm
import com.pitstop.models.Settings
import com.pitstop.network.RequestError
import com.pitstop.repositories.CarRepository
import com.pitstop.repositories.Repository
import com.pitstop.repositories.UserRepository
import java.text.DateFormatSymbols
import java.util.*

/**
 * Created by ishan on 2017-11-07.
 */
class GetAlarmCountUseCaseImpl (  val localAlarmStorage: LocalAlarmStorage,
                                val useCaseHandler: Handler, val mainHandler: Handler): GetAlarmCountUseCase {

    private val TAG = javaClass.simpleName;
    private var callback: GetAlarmCountUseCase.Callback? = null
    private var carid:Int = 0

    override fun execute(carid:Int,  callback: GetAlarmCountUseCase.Callback) {
        this.callback = callback
        this.carid = carid
        useCaseHandler.post(this)
    }

    override fun run() {
        localAlarmStorage.getAlarmCount(this.carid, object:  Repository.Callback<Int>{
            override fun onSuccess(data: Int?) {
                mainHandler.post({callback?.onAlarmCountGot(data!!)})
            }
            override fun onError(error: RequestError?) {
                mainHandler.post({callback?.onError(error!!)})
            }
        })
    }

    }