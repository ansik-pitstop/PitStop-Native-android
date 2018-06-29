package com.pitstop.interactors.other

import com.pitstop.interactors.Interactor
import com.pitstop.models.User
import com.pitstop.network.RequestError

/**
 * Created by Karol Zdebel on 6/15/2018.
 */
interface LoginFacebookUseCase: Interactor {

    interface Callback{
        fun onSuccess(user: User)
        fun onError(err: RequestError)
    }

    fun execute(facebookAuthToken: String, callback: Callback)
}