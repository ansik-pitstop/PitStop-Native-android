package com.pitstop.interactors.other

import com.pitstop.interactors.Interactor
import com.pitstop.network.RequestError

/**
 * Created by Karol Zdebel on 6/29/2018.
 */
interface ChangePasswordActivateUserUseCase: Interactor {

    interface Callback{
        fun onSuccess()
        fun onError(err: RequestError)
    }

    fun execute(oldPassword: String, newPassword: String, activateUser: Boolean, callback: Callback)
}