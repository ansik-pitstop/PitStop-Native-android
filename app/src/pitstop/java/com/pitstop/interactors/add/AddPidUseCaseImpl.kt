package com.pitstop.interactors.add

import android.os.Handler
import com.pitstop.bluetooth.dataPackages.PidPackage
import com.pitstop.models.DebugMessage
import com.pitstop.models.Settings
import com.pitstop.network.RequestError
import com.pitstop.repositories.CarRepository
import com.pitstop.repositories.Repository
import com.pitstop.repositories.SensorDataRepository
import com.pitstop.repositories.UserRepository
import com.pitstop.utils.Logger
import com.pitstop.utils.SensorDataUtils
import io.reactivex.schedulers.Schedulers

/**
 * Created by Karol Zdebel on 4/24/2018.
 */
class AddPidUseCaseImpl(private val sensorDataRepository: SensorDataRepository
                        , private val userRepository: UserRepository
                        , private val carRepository: CarRepository
                        , private val usecaseHandler: Handler
                        , private val mainHanler: Handler): AddPidUseCase {

    private val TAG = AddPidUseCaseImpl::class.java.simpleName

    private lateinit var callback: AddPidUseCase.Callback
    private lateinit var pidPackage: PidPackage
    private lateinit var vin: String

    override fun execute(pidPackage: PidPackage, vin: String, callback: AddPidUseCase.Callback) {
        Logger.getInstance().logI(TAG,"Use case execution started input: pidPackage=$pidPackage"
                , DebugMessage.TYPE_USE_CASE)
        this.pidPackage = pidPackage
        this.vin = vin
        this.callback = callback
        usecaseHandler.post(this)

    }

    override fun run() {
        if (vin.isEmpty()){
            userRepository.getCurrentUserSettings(object: Repository.Callback<Settings>{
                override fun onSuccess(data: Settings?) {
                    if (data == null) return
                    var usedLocalCar = false
                    carRepository.get(data.carId)
                            .observeOn(Schedulers.io())
                            .subscribeOn(Schedulers.computation())
                            .subscribe({car ->
                                if (car.data == null || usedLocalCar) return@subscribe
                                if (car.isLocal) usedLocalCar = true
                                sensorDataRepository.storeThenDump(SensorDataUtils.pidToSensorData(pidPackage,car.data.vin))
                                        .observeOn(Schedulers.io())
                                        .subscribeOn(Schedulers.computation())
                                        .subscribe({next ->
                                            onAdded()
                                        },{err ->
                                            onError(RequestError(err))
                                        })
                            },{err ->
                                onError(RequestError(err))
                            })
                }

                override fun onError(error: RequestError?) {
                    onError(error)
                }

            })
        }
        sensorDataRepository.storeThenDump(SensorDataUtils.pidToSensorData(pidPackage,vin))
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.computation())
                .subscribe({next ->
                    onAdded()
                },{err ->
                    onError(RequestError(err))
                })
    }

    private fun onAdded(){
        Logger.getInstance()!!.logI(TAG, "Use case finished: pids added successfully"
                , DebugMessage.TYPE_USE_CASE)
        mainHanler.post({callback.onAdded()})
    }

    private fun onError(err: RequestError){
        Logger.getInstance()!!.logE(TAG, "Use case finished: error = $err"
                , DebugMessage.TYPE_USE_CASE)
        mainHanler.post({callback.onError(err)})
    }

}