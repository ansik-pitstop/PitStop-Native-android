package com.pitstop.interactors.other

import com.pitstop.interactors.Interactor
import com.pitstop.network.RequestError

/**
 * Created by Karol Zdebel on 6/15/2018.
 */
interface LoginFacebookUseCase: Interactor {

    interface Callback{
        fun onSuccess()
        fun onError(err: RequestError)
    }

    fun execute(facebookAuthToken: String, callback: Callback)
}