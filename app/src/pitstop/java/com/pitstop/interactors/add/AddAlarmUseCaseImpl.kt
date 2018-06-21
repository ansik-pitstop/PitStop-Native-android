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
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

/**
 * Created by ishan on 2017-10-30.
 */
class AddAlarmUseCaseImpl (val userRepository: UserRepository, val carRepository: CarRepository, val localAlarmStorage: LocalAlarmStorage,
                           val useCaseHandler: Handler, val mainHandler: Handler) : AddAlarmUseCase {

    private val TAG = javaClass.simpleName;
    private var alarm : Alarm? = null;
    private var callback: AddAlarmUseCase.Callback? = null


    override fun execute(alarm: Alarm, callback: AddAlarmUseCase.Callback) {
        Logger.getInstance().logI(TAG, "Use case execution started, input alarm: "+alarm, DebugMessage.TYPE_USE_CASE)
        this.alarm = alarm
        this.callback = callback
        useCaseHandler.post(this);
    }

    private fun onError(err: RequestError){
        Logger.getInstance().logE(TAG, "Use case returned error: "+err, DebugMessage.TYPE_USE_CASE)
        mainHandler.post({callback!!.onError(err)})
    }

    private fun onAlarmAdded(alarm: Alarm){
        Logger.getInstance().logI(TAG, "Use case finished: "+alarm, DebugMessage.TYPE_USE_CASE)
        mainHandler.post({callback!!.onAlarmAdded(alarm)})
    }

    private fun onAlarmsDisabled(){
        Logger.getInstance().logI(TAG, "Use case finished: alarm disabled!", DebugMessage.TYPE_USE_CASE)
        mainHandler.post({callback!!.onAlarmsDisabled()})
    }

    override fun run() {
       userRepository.getCurrentUserSettings(object : Repository.Callback<Settings>{
           override fun onSuccess(settings: Settings) {
               if (!settings.hasMainCar()) {
                   this@AddAlarmUseCaseImpl.onError(RequestError.getUnknownError())
                   return
               }
               if (!settings.isAlarmsEnabled) {
                   this@AddAlarmUseCaseImpl.onAlarmsDisabled()
               } else {

                   if (settings.hasMainCar()) carRepository.get(settings.carId)
                           .subscribeOn(Schedulers.io())
                           .observeOn(AndroidSchedulers.from(useCaseHandler.getLooper()))
                           .subscribe({response ->
                               if (response.isLocal) return@subscribe
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
                   else AddAlarmUseCaseImpl@onError(RequestError.getUnknownError())
               }
           }
           override fun onError(error: RequestError) {
               this@AddAlarmUseCaseImpl.onError(error)
           }
       })
    }
}