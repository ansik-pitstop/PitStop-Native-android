package com.pitstop.interactors.other

import com.pitstop.interactors.Interactor
import com.pitstop.models.User
import com.pitstop.network.RequestError

/**
 * Created by Karol Zdebel on 6/15/2018.
 */
interface SignUpUseCase: Interactor {
    interface Callback{
        fun onSignedUp()
        fun onError(err: RequestError)
    }

    fun execute(user: User, callback: Callback)
}