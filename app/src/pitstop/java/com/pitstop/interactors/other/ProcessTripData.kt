package com.pitstop.interactors.other

import com.pitstop.interactors.Interactor

/**
 * Created by Karol Zdebel on 6/4/2018.
 */
interface ProcessTripDataUseCase: Interactor {

    interface Callback{
        fun processed()
    }

    fun execute(callback: Callback)
}