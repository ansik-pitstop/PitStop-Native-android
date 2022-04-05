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
import io.reactivex.disposables.CompositeDisposable
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
    private var compositeDisposable = CompositeDisposable()
    private var carId: Int = 0

    override fun execute(carId: Int, callback: GetAllAppointmentsUseCase.Callback) {
        Logger.getInstance()!!.logI(tag, "Use case execution started", DebugMessage.TYPE_USE_CASE)
        this.callback = callback
        this.carId = carId
        useCaseHandler.post(this)
    }

    private fun onError(err: RequestError?){
        compositeDisposable.clear()
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
        compositeDisposable.clear()
        mainHandler.post({callback!!.onGotAppointments(app)})
    }

    override fun run() {
        Log.d(tag,"run()")
        val disposable = appointmentRepository.getAllAppointments(this.carId)
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.io())
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
        compositeDisposable.add(disposable)
    }
}