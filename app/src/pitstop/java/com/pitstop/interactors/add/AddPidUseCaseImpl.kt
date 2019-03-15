package com.pitstop.interactors.add

import android.os.Handler
import android.util.Log
import com.pitstop.bluetooth.dataPackages.PidPackage
import com.pitstop.models.DebugMessage
import com.pitstop.models.Settings
import com.pitstop.models.sensor_data.SensorData
import com.pitstop.network.RequestError
import com.pitstop.repositories.*
import com.pitstop.utils.Logger
import com.pitstop.utils.SensorDataUtils
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

/**
 * Created by Karol Zdebel on 4/24/2018.
 */
class AddPidUseCaseImpl(private val sensorDataRepository: SensorDataRepository
                        , private val pidRepository: PidRepository
                        , private val userRepository: UserRepository
                        , private val carRepository: CarRepository
                        , private val usecaseHandler: Handler
                        , private val mainHanler: Handler): AddPidUseCase {

    private val TAG = AddPidUseCaseImpl::class.java.simpleName

    private lateinit var callback: AddPidUseCase.Callback
    private lateinit var pidPackage: PidPackage
    private lateinit var vin: String
    private var compositeDisposable = CompositeDisposable()

    override fun execute(pidPackage: PidPackage, vin: String, callback: AddPidUseCase.Callback) {
        Logger.getInstance().logI(TAG,"Use case execution started input: pidPackage=$pidPackage"
                , DebugMessage.TYPE_USE_CASE)
        this.pidPackage = pidPackage
        this.vin = vin
        this.callback = callback
        usecaseHandler.post(this)

    }

    override fun run() {
        val rows = pidRepository.store(pidPackage)
        Log.d(TAG,"stored $rows rows into Pid Repository!")
        //Get vin from car repo if empty
        if (vin.isEmpty()){
            userRepository.getCurrentUserSettings(object: Repository.Callback<Settings>{
                override fun onSuccess(data: Settings?) {
                    if (data == null) return
                    var usedLocalCar = false
                    if (data.hasMainCar()){
                        val disposable = carRepository.get(data.carId, Repository.DATABASE_TYPE.BOTH)
                                .observeOn(Schedulers.io())
                                .subscribeOn(Schedulers.computation())
                                .subscribe({car ->
                                    if (car.data == null || usedLocalCar) return@subscribe
                                    if (car.isLocal) usedLocalCar = true

                                    sendData(SensorDataUtils.pidToSensorData(pidPackage, car.data?.vin))

                                },{err ->
                                    this@AddPidUseCaseImpl.onError(RequestError(err))
                                })
                        compositeDisposable.add(disposable)
                    }
                    else this@AddPidUseCaseImpl.onError(RequestError.getUnknownError())
                }

                override fun onError(error: RequestError) {
                    this@AddPidUseCaseImpl.onError(error)
                }

            })
        }
        //Use already existing vin
        else{
            Log.d(TAG,"using already existing VIN")
            sendData(SensorDataUtils.pidToSensorData(pidPackage,vin))
        }
    }

    private fun onAdded(size: Int){
        Logger.getInstance()!!.logI(TAG, "Use case finished: pids sent to server successfully"
                , DebugMessage.TYPE_USE_CASE)
        compositeDisposable.clear()
        mainHanler.post({callback.onAdded(size)})
    }

    private fun onStoredLocally(size: Int){
        Logger.getInstance()!!.logI(TAG, "Use case finished: pids stored locally"
                , DebugMessage.TYPE_USE_CASE)
        compositeDisposable.clear()
        mainHanler.post({callback.onStoredLocally(size)})
    }

    private fun onError(err: RequestError){
        Logger.getInstance()!!.logE(TAG, "Use case finished: error = $err"
                , DebugMessage.TYPE_USE_CASE)
        compositeDisposable.clear()
        mainHanler.post({callback.onError(err)})
    }

    private fun sendData(sensorData: SensorData){
        val locallyStoredCount = sensorDataRepository
                .store(sensorData)

//        if (sensorDataRepository.getSensorDataCount()
//                >= sensorDataRepository.getChunkSize()){
        if (sensorDataRepository.getSensorDataCount()
                >= 2){

            val disposable = sensorDataRepository.dumpData()
                    .observeOn(Schedulers.io(), true)
                    .subscribeOn(Schedulers.computation())
                    .subscribe({next ->
                        onAdded(next)
                    },{err ->
                        err.printStackTrace()
                        this@AddPidUseCaseImpl.onError(RequestError(err))
                    })
            compositeDisposable.add(disposable)
        }else{
            onStoredLocally(locallyStoredCount)
        }
    }

}