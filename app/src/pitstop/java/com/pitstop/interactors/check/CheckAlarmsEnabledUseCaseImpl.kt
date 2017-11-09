package com.pitstop.interactors.check

import android.os.Handler
import com.pitstop.models.Settings
import com.pitstop.network.RequestError
import com.pitstop.repositories.Repository
import com.pitstop.repositories.UserRepository

/**
 * Created by ishan on 2017-11-02.
 */
class CheckAlarmsEnabledUseCaseImpl(val userRepository: UserRepository, val useCaseHandler: Handler,
                                    private val mainHandler: Handler): CheckAlarmsEnabledUse {
    var callback: CheckAlarmsEnabledUse.Callback? = null

    override fun execute(callback: CheckAlarmsEnabledUse.Callback) {
        this.callback = callback
        useCaseHandler.post(this)
    }

    override fun run() {
        userRepository.getCurrentUserSettings(object : Repository.Callback<Settings>{

            override fun onSuccess(data: Settings?) {
                callback?.onAlarmsEnabledChecked(data?.isAlarmsEnabled!!)

            }

            override fun onError(error: RequestError?) {
                callback?.onError(error!!);
            }
        })
    }
}