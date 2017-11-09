package com.pitstop.interactors.add

import android.os.Handler
import android.util.Log
import com.pitstop.database.LocalAlarmStorage
import com.pitstop.models.Alarm
import com.pitstop.models.Settings
import com.pitstop.network.RequestError
import com.pitstop.repositories.CarRepository
import com.pitstop.repositories.Repository
import com.pitstop.repositories.RepositoryResponse
import com.pitstop.repositories.UserRepository
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
        Log.d(TAG, "execute() " )
        this.alarm = alarm
        this.callback = callback
        useCaseHandler.post(this);
    }

    override fun run() {
       userRepository.getCurrentUserSettings(object : Repository.Callback<Settings>{
           override fun onSuccess(settings: Settings) {
               if (!settings.hasMainCar()) {
                   callback?.onError(RequestError.getUnknownError());
                   return
               }
               if (!settings.isAlarmsEnabled) {
                   mainHandler.post({ callback?.onAlarmsDisabled() })
               } else {

                   carRepository.get(settings.carId)
                           .subscribeOn(Schedulers.io())
                           .observeOn(Schedulers.computation())
                           .doOnNext{response ->
                               val car = response.data
                               if (car == null){
                                   callback!!.onError(RequestError.getUnknownError())
                                   return@doOnNext
                               }
                               alarm?.carID = car.id
                               localAlarmStorage.storeAlarm(alarm,
                                       object : Repository.Callback<Alarm> {
                                           override fun onSuccess(alarm: Alarm?) {
                                               Log.d(TAG, "store alarm on success")
                                               mainHandler.post({ callback?.onAlarmAdded(alarm!!) })
                                           }
                                           override fun onError(error: RequestError) {
                                               Log.d(TAG, "Error adding alarm:  " + error.message)
                                               mainHandler.post({ callback?.onError(error) })
                                           }
                                       })

                           }.onErrorReturn { err -> RepositoryResponse(null,false) }
                           .subscribe()
               }
           }
           override fun onError(error: RequestError?) {
               Log.d(TAG,"Error storing alarm: ")
               mainHandler.post({callback?.onError(error!!)})
           }
       })
    }
}