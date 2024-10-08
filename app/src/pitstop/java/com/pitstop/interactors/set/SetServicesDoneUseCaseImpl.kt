package com.pitstop.interactors.set

import android.os.Handler
import android.util.Log
import com.pitstop.EventBus.EventBusNotifier
import com.pitstop.EventBus.EventSource
import com.pitstop.EventBus.EventType
import com.pitstop.EventBus.EventTypeImpl
import com.pitstop.models.DebugMessage
import com.pitstop.models.issue.CarIssue
import com.pitstop.network.RequestError
import com.pitstop.repositories.CarIssueRepository
import com.pitstop.utils.Logger
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

/**
 * Created by Karol Zdebel on 10/20/2017.
 */
class SetServicesDoneUseCaseImpl(val carIssueRepository: CarIssueRepository
                                 ,val useCaseHandler: Handler,val mainHandler: Handler): SetServicesDoneUseCase {

    val tag: String? = SetServicesDoneUseCaseImpl::class.java.simpleName
    var carIssues: List<CarIssue>? = null
    var eventSource: EventSource? = null
    var callback: SetServicesDoneUseCase.Callback? = null
    var compositeDisposable = CompositeDisposable()

    override fun execute(carIssues: List<CarIssue>, eventSource: EventSource, callback: SetServicesDoneUseCase.Callback) {
        Logger.getInstance()!!.logI(tag, "Use case execution started: carIssues="+carIssues
                , DebugMessage.TYPE_USE_CASE)
        this.carIssues = carIssues
        this.eventSource = eventSource
        this.callback = callback
        useCaseHandler.post(this)
    }

    override fun run() {
        Log.d(tag,"run()")
        val issuesIterator = carIssues!!.iterator()
        while (issuesIterator.hasNext()){
            val issue = issuesIterator.next()
            issue.status = CarIssue.ISSUE_DONE
            val hasNext = issuesIterator.hasNext()
            Log.d(tag,"calling markDone() on issue: $issue")
            val disposable = carIssueRepository.markDone(issue)
                    .subscribeOn(Schedulers.computation())
                    .observeOn(Schedulers.io())
                    .subscribe({next ->
                        issue.doneAt = next.doneAt
                        issue.status = next.status
                        Log.d(tag,"markDone, hasNext? $hasNext, onSuccess, issue: $issue")
                        mainHandler.post({callback!!.onServiceMarkedAsDone(issue)})
                        if (!hasNext){
                            EventBusNotifier.notifyCarDataChanged(
                                    EventTypeImpl(EventType.EVENT_SERVICES_HISTORY), eventSource)
                            mainHandler.post({callback!!.onComplete()})
                            Logger.getInstance()!!.logI(tag, "Use case finished"
                                    , DebugMessage.TYPE_USE_CASE)
                        }
                    }, {error ->
                        error.printStackTrace()
                        Logger.getInstance()!!.logI(tag, "Use case returned error: err=$error"
                                , DebugMessage.TYPE_USE_CASE)
                        mainHandler.post({callback!!.onError(RequestError(error))})
                    })
                    compositeDisposable.add(disposable)
        }

    }


}