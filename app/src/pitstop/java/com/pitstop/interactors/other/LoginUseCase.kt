package com.pitstop.interactors.other

import com.pitstop.interactors.Interactor
import com.pitstop.models.User
import com.pitstop.network.RequestError

/**
 * Created by Karol Zdebel on 6/15/2018.
 */
interface LoginUseCase: Interactor {

    interface Callback{
        fun onSuccess(user: User, activated: Boolean)
        fun onError(error: RequestError)
    }

    fun execute(username: String, password: String, callback: Callback)
}