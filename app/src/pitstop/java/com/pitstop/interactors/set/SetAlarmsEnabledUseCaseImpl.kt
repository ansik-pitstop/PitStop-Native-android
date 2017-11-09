package com.pitstop.interactors.set

import android.os.Handler
import android.util.Log
import com.pitstop.network.RequestError
import com.pitstop.repositories.Repository
import com.pitstop.repositories.UserRepository

/**
 * Created by ishan on 2017-11-02.
 */
class SetAlarmsEnabledUseCaseImpl(val userRepository: UserRepository, val useCaseHandler: Handler,
                                  private val mainHandler: Handler): SetAlarmsEnabledUseCase {
    var callback: SetAlarmsEnabledUseCase.Callback? = null
    var alarmsEnabled: Boolean = false

    override fun execute(alarmsEnabled: Boolean, callback: SetAlarmsEnabledUseCase.Callback) {
        this.alarmsEnabled = alarmsEnabled
        this.callback = callback
        useCaseHandler.post(this)
    }

    override fun run() {
        userRepository.setAlarmsEnabled(alarmsEnabled, object : Repository.Callback<Any> {
            override fun onSuccess(data: Any?) {
                mainHandler.post({callback?.onAlarmsEnabledSet()})
            }
            override fun onError(error: RequestError?) {
                mainHandler.post({callback?.onError(error!!)})
            }
        })
    }
}