package com.pitstop.interactors.get

import android.os.Handler
import android.util.Log
import com.pitstop.models.Settings
import com.pitstop.network.RequestError
import com.pitstop.repositories.AppointmentRepository
import com.pitstop.repositories.Repository
import com.pitstop.repositories.UserRepository
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

/**
 * Created by Karol Zdebel on 2/5/2018.
 */
class GetAllAppointmentsUseCaseImpl(private val appointmentRepository: AppointmentRepository
                                    , private val userRepository: UserRepository
                                    , private val useCaseHandler: Handler
                                    , private val mainHandler: Handler): GetAllAppointmentsUseCase {

    private val tag = javaClass.simpleName
    private var callback: GetAllAppointmentsUseCase.Callback? = null

    override fun execute(callback: GetAllAppointmentsUseCase.Callback) {
        Log.d(tag,"execute()")
        this.callback = callback
        useCaseHandler.post(this)
    }

    override fun run() {
        Log.d(tag,"run()")
        userRepository.getCurrentUserSettings(object:  Repository.Callback<Settings>{
            override fun onSuccess(data: Settings?) {
                Log.d(tag,"userRepository.getCurrentUserSettings success: "+data)
                if (data == null){
                    mainHandler.post({callback!!.onError(RequestError.getUnknownError())})
                    return
                }
                appointmentRepository.getAllAppointments(data.carId)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.from(useCaseHandler.getLooper()))
                        .doOnNext({next ->
                            Log.d(tag,"appointmentRepository.onNext() data: "+next)
                            mainHandler.post({callback!!.onGotAppointments(next)})
                            return@doOnNext
                        }).onErrorResumeNext({error: Throwable ->
                            Log.d(tag,"appointmentRepository.onErrorResumeNext() error: "+error)
                            mainHandler.post({callback!!.onError(RequestError(error))})
                        null
                        }).subscribe()
            }

            override fun onError(error: RequestError?) {
                if (error != null)
                    mainHandler.post({callback!!.onError(error)})
                else mainHandler.post({callback!!.onError(RequestError.getUnknownError())})
            }
        })
    }
}