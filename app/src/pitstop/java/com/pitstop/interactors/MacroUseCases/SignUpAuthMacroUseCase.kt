package com.pitstop.interactors.MacroUseCases

import com.pitstop.models.User
import com.pitstop.network.RequestError

/**
 * Created by Karol Zdebel on 6/22/2018.
 */
interface SignUpAuthMacroUseCase {
    interface Callback{
        fun onSuccess()
        fun onError(err: RequestError)
    }

    fun execute(user: User, smoochUser: io.smooch.core.User, callback: Callback)
}