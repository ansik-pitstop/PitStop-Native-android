package com.pitstop.interactors.get

import android.os.Handler
import android.util.Log
import com.pitstop.models.DebugMessage
import com.pitstop.models.Settings
import com.pitstop.models.issue.CarIssue
import com.pitstop.network.RequestError
import com.pitstop.repositories.*
import com.pitstop.utils.Logger
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

/**
 * Created by Karol Zdebel on 5/31/2017.
 */

class GetDoneServicesUseCaseImpl(private val userRepository: UserRepository, private val carIssueRepository: CarIssueRepository, private val carRepository: CarRepository, private val useCaseHandler: Handler, private val mainHandler: Handler) : GetDoneServicesUseCase {

    private val TAG = javaClass.simpleName
    private var callback: GetDoneServicesUseCase.Callback? = null
    private val compositeDisposable = CompositeDisposable()
    private var carId: Int = 0

    override fun execute(carId: Int, callback: GetDoneServicesUseCase.Callback) {
        Logger.getInstance()!!.logI(TAG, "Use case execution started", DebugMessage.TYPE_USE_CASE)
        this.callback = callback
        this.carId = carId
        useCaseHandler.post(this)
    }

    private fun onGotDoneServices(doneServices: List<CarIssue>?, isLocal: Boolean) {
        Logger.getInstance()!!.logI(TAG, "Use case finished: doneServices=" + doneServices!!, DebugMessage.TYPE_USE_CASE)
        if (!isLocal) {
            compositeDisposable.clear()
        }
        mainHandler.post { callback!!.onGotDoneServices(doneServices, isLocal) }
    }

    private fun onNoCarAdded(isLocal: Boolean) {
        Logger.getInstance()!!.logI(TAG, "Use case finished: no car added!", DebugMessage.TYPE_USE_CASE)
        if (!isLocal) {
            compositeDisposable.clear()
        }
        mainHandler.post { callback!!.onNoCarAdded() }
    }

    private fun onError(error: RequestError) {
        Logger.getInstance()!!.logE(TAG, "Use case returned error: err=$error", DebugMessage.TYPE_USE_CASE)
        compositeDisposable.clear()
        mainHandler.post { callback!!.onError(error) }
    }

    override fun run() {
        getDoneCarIssues(this.carId)
    }

    private fun getDoneCarIssues(carId: Int) {
        //Use the current users car to get all the current issues
        val disposable = carIssueRepository.getDoneCarIssues(carId, Repository.DATABASE_TYPE.BOTH)
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.io(), true)
                .subscribe({ next ->
                    this@GetDoneServicesUseCaseImpl.onGotDoneServices(next.data, next.isLocal)
                }) { error ->
                    error.printStackTrace()
                    this@GetDoneServicesUseCaseImpl.onError(RequestError(error))
                }
        compositeDisposable.add(disposable)
    }
}
