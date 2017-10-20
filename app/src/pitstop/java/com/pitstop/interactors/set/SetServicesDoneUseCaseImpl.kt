package com.pitstop.interactors.set

import android.os.Handler
import android.util.Log
import com.pitstop.EventBus.EventBusNotifier
import com.pitstop.EventBus.EventSource
import com.pitstop.EventBus.EventType
import com.pitstop.EventBus.EventTypeImpl
import com.pitstop.models.issue.CarIssue
import com.pitstop.network.RequestError
import com.pitstop.repositories.CarIssueRepository
import com.pitstop.repositories.Repository

/**
 * Created by Karol Zdebel on 10/20/2017.
 */
class SetServicesDoneUseCaseImpl(val carIssueRepository: CarIssueRepository
                                 ,val useCaseHandler: Handler,val mainHandler: Handler): SetServicesDoneUseCase {

    val tag: String? = javaClass.simpleName
    var carIssues: List<CarIssue>? = null
    var eventSource: EventSource? = null
    var callback: SetServicesDoneUseCase.Callback? = null

    override fun execute(carIssues: List<CarIssue>, eventSource: EventSource, callback: SetServicesDoneUseCase.Callback) {
        this.carIssues = carIssues
        this.eventSource = eventSource
        this.callback = callback
        useCaseHandler.post(this)
    }

    override fun run() {
        val issuesIterator = carIssues!!.iterator()
        while (issuesIterator.hasNext()){
            val hasNext = issuesIterator.hasNext()
            val issue = issuesIterator.next()
            Log.d(tag,"calling updateCarIssue() on issue: $issue")
            carIssueRepository.updateCarIssue(issue, object : Repository.Callback<CarIssue>{
                override fun onSuccess(carIssueReturned: CarIssue) {
                    issue.doneAt = carIssueReturned.doneAt
                    issue.status = carIssueReturned.status
                    Log.d(tag,"updateCarIssue, hasNext? $hasNext, onSuccess, issue: $issue")
                    mainHandler.post({callback!!.onServiceMarkedAsDone(issue)})
                    if (!hasNext){
                        EventBusNotifier.notifyCarDataChanged(
                                EventTypeImpl(EventType.EVENT_SERVICES_HISTORY), eventSource)
                        mainHandler.post({callback!!.onComplete()})
                    }
                }

                override fun onError(error: RequestError) {
                    mainHandler.post({callback!!.onError(error)})
                }
            })
        }

    }


}