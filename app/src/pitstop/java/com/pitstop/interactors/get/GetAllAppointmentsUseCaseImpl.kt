package com.pitstop.interactors.get

import android.os.Handler
import android.util.Log
import com.pitstop.models.Appointment
import com.pitstop.models.DebugMessage
import com.pitstop.models.Settings
import com.pitstop.network.RequestError
import com.pitstop.repositories.AppointmentRepository
import com.pitstop.repositories.Repository
import com.pitstop.repositories.UserRepository
import com.pitstop.utils.Logger
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
        Logger.getInstance()!!.logI(tag, "Use case execution started", DebugMessage.TYPE_USE_CASE)
        this.callback = callback
        useCaseHandler.post(this)
    }

    private fun onError(err: RequestError?){
        if (err != null){
            Logger.getInstance()!!.logE(tag, "Use case returned error: err=" + err, DebugMessage.TYPE_USE_CASE)
            mainHandler.post({callback!!.onError(err)})
        }else{
            Logger.getInstance()!!.logE(tag, "Use case returned error: err="
                    + RequestError.getUnknownError(), DebugMessage.TYPE_USE_CASE)
            mainHandler.post({callback!!.onError(RequestError.getUnknownError())})
        }
    }

    private fun onGotAllAppointments(app: List<Appointment>){
        Logger.getInstance()!!.logI(tag, "Use case finished result: appointments=$app", DebugMessage.TYPE_USE_CASE)
        mainHandler.post({callback!!.onGotAppointments(app)})
    }

    override fun run() {
        Log.d(tag,"run()")
        userRepository.getCurrentUserSettings(object:  Repository.Callback<Settings>{
            override fun onSuccess(data: Settings?) {
                Log.d(tag, "userRepository.getCurrentUserSettings success: " + data)
                if (data == null) {
                    this@GetAllAppointmentsUseCaseImpl.onError(com.pitstop.network.RequestError.getUnknownError())
                    return
                }
                appointmentRepository.getAllAppointments(data.carId)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.from(useCaseHandler.getLooper()))
                        .subscribe(
                        { next ->
                            Log.d(tag, "appointmentRepository.onNext() data: " + next)
                            this@GetAllAppointmentsUseCaseImpl.onGotAllAppointments(next)
                        },
                        { error: Throwable ->
                            Log.d(tag, "appointmentRepository.onErrorResumeNext() error: " + error)
                            this@GetAllAppointmentsUseCaseImpl.onError(RequestError(error))
                        }
                )
            }

            override fun onError(error: RequestError?) {
                this@GetAllAppointmentsUseCaseImpl.onError(error)
            }
        })
    }
}