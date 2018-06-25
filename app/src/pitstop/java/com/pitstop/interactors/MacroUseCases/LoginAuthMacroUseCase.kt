package com.pitstop.interactors.MacroUseCases

import com.pitstop.network.RequestError

/**
 * Created by Karol Zdebel on 6/22/2018.
 */
interface LoginAuthMacroUseCase {
    interface Callback{
        fun onSuccess()
        fun onError(err: RequestError)
    }

    fun execute(username: String, password: String, smoochUser: io.smooch.core.User, callback: Callback)
}