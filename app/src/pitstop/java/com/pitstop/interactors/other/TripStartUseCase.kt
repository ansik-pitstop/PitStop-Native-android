package com.pitstop.interactors.other

import com.pitstop.interactors.Interactor

/**
 * Created by Karol Zdebel on 3/28/2018.
 */
interface TripStartUseCase: Interactor {
    interface Callback{
        fun finished()
    }

    fun execute(callback: Callback)
}