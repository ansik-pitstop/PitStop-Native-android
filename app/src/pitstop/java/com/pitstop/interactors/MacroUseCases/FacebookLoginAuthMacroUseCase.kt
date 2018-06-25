package com.pitstop.interactors.MacroUseCases

import com.pitstop.network.RequestError
import io.smooch.core.User

/**
 * Created by Karol Zdebel on 6/22/2018.
 */
interface FacebookLoginAuthMacroUseCase {
    interface Callback{
        fun onSuccess()
        fun onError(err: RequestError)
    }

    fun execute(facebookAuthToken: String, smoochUser: User, callback: Callback)
}