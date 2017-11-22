package com.pitstop.interactors.set

import android.os.Handler
import com.pitstop.models.DebugMessage
import com.pitstop.network.RequestError
import com.pitstop.repositories.Repository
import com.pitstop.repositories.UserRepository
import com.pitstop.utils.Logger

/**
 * Created by ishan on 2017-11-02.
 */
class SetAlarmsEnabledUseCaseImpl(val userRepository: UserRepository, val useCaseHandler: Handler,
                                  private val mainHandler: Handler): SetAlarmsEnabledUseCase {
    private var callback: SetAlarmsEnabledUseCase.Callback? = null
    private var alarmsEnabled: Boolean = false
    private val tag = javaClass.simpleName

    override fun execute(alarmsEnabled: Boolean, callback: SetAlarmsEnabledUseCase.Callback) {
        Logger.getInstance()!!.logI(tag, "Use case execution started: alarmsEnabled=$alarmsEnabled"
                , DebugMessage.TYPE_USE_CASE)
        this.alarmsEnabled = alarmsEnabled
        this.callback = callback
        useCaseHandler.post(this)
    }

    override fun run() {
        userRepository.setAlarmsEnabled(alarmsEnabled, object : Repository.Callback<Any> {
            override fun onSuccess(data: Any?) {
                Logger.getInstance()!!.logI(tag, "Use case finished"
                        , DebugMessage.TYPE_USE_CASE)
                mainHandler.post({callback?.onAlarmsEnabledSet()})
            }
            override fun onError(error: RequestError?) {
                Logger.getInstance()!!.logI(tag, "Use case returned error: err=$error"
                        , DebugMessage.TYPE_USE_CASE)
                mainHandler.post({callback?.onError(error!!)})
            }
        })
    }
}