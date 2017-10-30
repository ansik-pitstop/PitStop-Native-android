package com.pitstop.interactors.get

import com.pitstop.interactors.Interactor
import com.pitstop.models.Dealership
import com.pitstop.models.issue.CarIssue
import com.pitstop.network.RequestError

/**
 * Created by Karol Zdebel on 10/16/2017.
 */
interface GetDealershipWithCarIssuesUseCase: Interactor {
    interface Callback{
        fun onGotDealershipAndIssues(dealership: Dealership, carIssues: List<CarIssue>)
        fun onError(error: RequestError)
    }

    fun execute(callback: Callback)
}