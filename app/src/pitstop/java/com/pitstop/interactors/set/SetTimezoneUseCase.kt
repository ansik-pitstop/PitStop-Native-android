package com.pitstop.interactors.set

import com.pitstop.interactors.Interactor

interface SetTimezoneUseCase: Interactor {
    interface Callback{
        fun onComplete()
    }

    fun execute(callback: Callback)
}