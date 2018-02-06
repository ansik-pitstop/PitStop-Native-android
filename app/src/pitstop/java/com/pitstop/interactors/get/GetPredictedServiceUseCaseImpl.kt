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
import io.reactivex.android.schedulers.AndroidSchedulers
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

    private fun onError(error: RequestError?){
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
        mainHandler.post({callback!!.onGotPredictedService(predictedService)})
    }

    override fun execute(callback: GetPredictedServiceUseCase.Callback) {
        Logger.getInstance()!!.logI(tag, "Use case started execution", DebugMessage.TYPE_USE_CASE)
        this.callback = callback
        usecaseHandler.post(this)
    }

    override fun run() {
        Log.d(tag,"run()")
        userRepository.getCurrentUserSettings(object: Repository.Callback<Settings>{
            override fun onSuccess(data: Settings?) {
                Log.d(tag,"Got settings: "+data);
                if (data == null) this@GetPredictedServiceUseCaseImpl.onError(com.pitstop.network.RequestError.getUnknownError())

                appointmentRepository.getPredictedService(data!!.carId)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.from(usecaseHandler.getLooper()))
                        .subscribe({response ->
                            this@GetPredictedServiceUseCaseImpl.onGotPredictedService(response)
                        },{error ->
                            this@GetPredictedServiceUseCaseImpl.onError(RequestError(error))
                        })
            }

            override fun onError(error: RequestError?) {
                Log.d(tag,"Error getting settings err: "+error)
                this@GetPredictedServiceUseCaseImpl.onError(error)
            }

        })
    }
}