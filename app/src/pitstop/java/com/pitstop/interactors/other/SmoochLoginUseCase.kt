package com.pitstop.interactors.other

import com.pitstop.interactors.Interactor

/**
 * Created by Karol Zdebel on 2/26/2018.
 */
interface SmoochLoginUseCase: Interactor {
    interface Callback{
        fun onError(err: String)
        fun onLogin()
    }

    fun execute(userId: String, callback: Callback)
}