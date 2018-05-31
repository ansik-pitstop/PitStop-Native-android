package com.pitstop.interactors.other

import com.pitstop.interactors.Interactor
import com.pitstop.models.PendingUpdate
import com.pitstop.network.RequestError

/**
 * Created by Karol Zdebel on 5/28/2018.
 */
interface SendPendingUpdatesUseCase: Interactor {
    interface Callback{
        fun updatesSent(pendingUpdates: List<PendingUpdate>)
        fun errorSending(err: RequestError)
    }

    fun execute(callback: Callback)
}