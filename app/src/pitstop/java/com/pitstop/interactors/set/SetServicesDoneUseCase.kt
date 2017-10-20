package com.pitstop.interactors.set

import com.pitstop.EventBus.EventSource
import com.pitstop.interactors.Interactor
import com.pitstop.models.issue.CarIssue
import com.pitstop.network.RequestError

/**
 * Created by Karol Zdebel on 10/20/2017.
 */
interface SetServicesDoneUseCase: Interactor {
    interface Callback{
        fun onServiceMarkedAsDone(carIssue: CarIssue)
        fun onComplete()
        fun onError(error: RequestError)
    }

    fun execute(carIssues: List<CarIssue>, eventSource: EventSource, callback: Callback)
}