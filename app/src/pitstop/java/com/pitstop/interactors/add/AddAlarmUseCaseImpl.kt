package com.pitstop.interactors.add

import android.os.Handler
import com.pitstop.database.LocalAlarmStorage
import com.pitstop.models.Alarm
import com.pitstop.models.DebugMessage
import com.pitstop.models.Settings
import com.pitstop.network.RequestError
import com.pitstop.repositories.CarRepository
import com.pitstop.repositories.Repository
import com.pitstop.repositories.UserRepository
import com.pitstop.utils.Logger
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

/**
 * Created by ishan on 2017-10-30.
 */
class AddAlarmUseCaseImpl (val userRepository: UserRepository, val carRepository: CarRepository, val localAlarmStorage: LocalAlarmStorage,
                           val useCaseHandler: Handler, val mainHandler: Handler) : AddAlarmUseCase {

    private val TAG = javaClass.simpleName;
    private var alarm : Alarm? = null;
    private var callback: AddAlarmUseCase.Callback? = null
    private var compositeDisposable = CompositeDisposable()
    private var carId: Int = 0

    override fun execute(carId: Int, alarm: Alarm, callback: AddAlarmUseCase.Callback) {
        Logger.getInstance().logI(TAG, "Use case execution started, input alarm: "+alarm, DebugMessage.TYPE_USE_CASE)
        this.alarm = alarm
        this.callback = callback
        this.carId = carId
        useCaseHandler.post(this)
    }

    private fun onError(err: RequestError){
        Logger.getInstance().logE(TAG, "Use case returned error: "+err, DebugMessage.TYPE_USE_CASE)
        compositeDisposable.clear()
        mainHandler.post({callback!!.onError(err)})
    }

    private fun onAlarmAdded(alarm: Alarm){
        Logger.getInstance().logI(TAG, "Use case finished: "+alarm, DebugMessage.TYPE_USE_CASE)
        compositeDisposable.clear()
        mainHandler.post({callback!!.onAlarmAdded(alarm)})
    }

    private fun onAlarmsDisabled(){
        Logger.getInstance().logI(TAG, "Use case finished: alarm disabled!", DebugMessage.TYPE_USE_CASE)
        compositeDisposable.clear()
        mainHandler.post({callback!!.onAlarmsDisabled()})
    }

    override fun run() {
        val disposable = carRepository.get(this.carId, Repository.DATABASE_TYPE.REMOTE)
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.io())
                .subscribe({response ->
                    val car = response.data
                    if (car == null){
                        callback!!.onError(RequestError.getUnknownError())
                        return@subscribe
                    }
                    alarm?.carID = car.id
                    localAlarmStorage.storeAlarm(alarm,
                            object : Repository.Callback<Alarm> {
                                override fun onSuccess(alarm: Alarm?) {
                                    this@AddAlarmUseCaseImpl.onAlarmAdded(alarm!!)
                                }
                                override fun onError(error: RequestError) {
                                    this@AddAlarmUseCaseImpl.onError(error)
                                }
                            })

                },{ err -> this@AddAlarmUseCaseImpl.onError(RequestError(err)) })
        compositeDisposable.add(disposable)
    }
}