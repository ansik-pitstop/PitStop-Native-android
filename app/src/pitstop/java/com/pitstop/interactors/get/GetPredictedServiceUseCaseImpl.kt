package com.pitstop.interactors.get

import android.os.Handler
import android.util.Log
import com.pitstop.models.DebugMessage
import com.pitstop.models.Settings
import com.pitstop.network.RequestError
import com.pitstop.repositories.AppointmentRepository
import com.pitstop.repositories.Repository
import com.pitstop.repositories.UserRepository
import com.pitstop.retrofit.PredictedService
import com.pitstop.utils.Logger
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

/**
 * Created by Karol Zdebel on 2/6/2018.
 */
class GetPredictedServiceUseCaseImpl(private val userRepository: UserRepository
                                     , private val appointmentRepository: AppointmentRepository
                                     , private val usecaseHandler: Handler
                                     , private val mainHandler: Handler): GetPredictedServiceUseCase{

    private val tag = javaClass.simpleName
    private var callback: GetPredictedServiceUseCase.Callback? = null
    private val compositeDisposable = CompositeDisposable()
    private var carId: Int = 0

    private fun onError(error: RequestError?){
        compositeDisposable.clear()
        if (error != null){
            Logger.getInstance()!!.logE(tag, "Use case returned error: err="
                    + error, DebugMessage.TYPE_USE_CASE)
            mainHandler.post({callback!!.onError(error)})
        }else{
            Logger.getInstance()!!.logE(tag, "Use case returned error: err="
                    + RequestError.getUnknownError(), DebugMessage.TYPE_USE_CASE)
            mainHandler.post({callback!!.onError(RequestError.getUnknownError())})
        }
    }

    private fun onGotPredictedService(predictedService: PredictedService){
        Logger.getInstance()!!.logI(tag, "Use case finished: predictedService="
                + predictedService, DebugMessage.TYPE_USE_CASE)
        compositeDisposable.clear()
        mainHandler.post({callback!!.onGotPredictedService(predictedService)})
    }

    override fun execute(carId: Int, callback: GetPredictedServiceUseCase.Callback) {
        Logger.getInstance()!!.logI(tag, "Use case started execution", DebugMessage.TYPE_USE_CASE)
        this.callback = callback
        usecaseHandler.post(this)
    }

    override fun run() {
        Log.d(tag,"run()")
        val disposable = appointmentRepository.getPredictedService(this.carId)
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.io())
                .subscribe({response ->
                    this@GetPredictedServiceUseCaseImpl.onGotPredictedService(response)
                },{error ->
                    Log.e(tag,"Error: "+error);
                    this@GetPredictedServiceUseCaseImpl.onError(RequestError(error))
                })
        compositeDisposable.add(disposable)
    }
}