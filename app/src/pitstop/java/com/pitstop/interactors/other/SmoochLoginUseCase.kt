package com.pitstop.interactors.other

import com.pitstop.interactors.Interactor
import com.pitstop.network.RequestError

/**
 * Created by Karol Zdebel on 2/26/2018.
 */
interface SmoochLoginUseCase: Interactor {
    interface Callback{
        fun onError(err: RequestError)
        fun onLogin()
    }

    fun execute(smoochUser: io.smooch.core.User, callback: Callback)
}