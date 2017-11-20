package com.pitstop.interactors.check

import android.os.Handler
import com.pitstop.models.DebugMessage
import com.pitstop.models.Settings
import com.pitstop.network.RequestError
import com.pitstop.repositories.Repository
import com.pitstop.repositories.UserRepository
import com.pitstop.utils.Logger

/**
 * Created by ishan on 2017-11-02.
 */
class CheckAlarmsEnabledUseCaseImpl(val userRepository: UserRepository, val useCaseHandler: Handler,
                                    private val mainHandler: Handler): CheckAlarmsEnabledUse {
    var callback: CheckAlarmsEnabledUse.Callback? = null
    val tag = javaClass.simpleName

    override fun execute(callback: CheckAlarmsEnabledUse.Callback) {
        Logger.getInstance()!!.logI(tag, "Use case execution started", false, DebugMessage.TYPE_USE_CASE)
        this.callback = callback
        useCaseHandler.post(this)
    }

    override fun run() {
        userRepository.getCurrentUserSettings(object : Repository.Callback<Settings>{

            override fun onSuccess(data: Settings?) {
                Logger.getInstance()!!.logI(tag, "Use case finished: result="+data?.isAlarmsEnabled!!
                        , false, DebugMessage.TYPE_USE_CASE)
                callback?.onAlarmsEnabledChecked(data?.isAlarmsEnabled!!)

            }

            override fun onError(error: RequestError?) {
                Logger.getInstance()!!.logI(tag, "Use case returned error: err="+error
                        , false, DebugMessage.TYPE_USE_CASE)
                callback?.onError(error!!)
            }
        })
    }
}