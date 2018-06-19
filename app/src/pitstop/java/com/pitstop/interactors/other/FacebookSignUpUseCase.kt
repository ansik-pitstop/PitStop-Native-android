package com.pitstop.interactors.other

import com.pitstop.interactors.Interactor
import com.pitstop.models.User
import com.pitstop.network.RequestError

/**
 * Created by Karol Zdebel on 6/19/2018.
 */
interface FacebookSignUpUseCase: Interactor {

    interface Callback{
        fun onSuccess()
        fun onError(err: RequestError)
    }

    fun execute(facebookAuthToken: String, callback: Callback)
}