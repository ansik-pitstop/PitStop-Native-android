package com.pitstop.interactors.add

import android.os.Handler
import android.util.Log
import com.pitstop.bluetooth.dataPackages.DtcPackage
import com.pitstop.models.Car
import com.pitstop.models.Settings
import com.pitstop.network.RequestError
import com.pitstop.repositories.CarIssueRepository
import com.pitstop.repositories.CarRepository
import com.pitstop.repositories.Repository
import com.pitstop.repositories.UserRepository
import com.pitstop.utils.PeriodicDtcSender

/**
 * Created by Karol Zdebel on 10/11/2017.
 */
class AddDtcUseCaseImpl(val userRepository: UserRepository, val carIssueRepository: CarIssueRepository
                        , val carRepository: CarRepository, val useCaseHandler: Handler, val mainHandler: Handler) : AddDtcUseCase {

    private val TAG = javaClass.simpleName
    private var dtcPackage: DtcPackage? = null
    private var callback: AddDtcUseCase.Callback? = null

    override fun execute(dtcPackage: DtcPackage, callback: AddDtcUseCase.Callback) {

        this.dtcPackage = dtcPackage
        this.callback = callback
        useCaseHandler.post(this)
    }

    override fun run() {
        userRepository.getCurrentUserSettings(object : Repository.Callback<Settings> {

            override fun onSuccess(settings: Settings){

                if (!settings.hasMainCar()){
                    callback?.onError(RequestError.getUnknownError());
                    return
                }

                carRepository.get(settings.carId, settings.userId, object : Repository.Callback<Car> {

                    override fun onSuccess(car: Car){

                        for ((dtc, isPending) in dtcPackage!!.dtcs){
                            carIssueRepository.insertDtc(settings.carId, car.totalMileage
                                    , dtcPackage?.rtcTime!!.toLong(), dtc, isPending, object : Repository.Callback<Any> {

                                override fun onSuccess(response: Any){
                                    Log.d(TAG,"successfully added dtc response: "+response)
                                    mainHandler.post({callback?.onDtcPackageAdded(dtcPackage as DtcPackage)})

                                }
                                override fun onError(error: RequestError){
                                    Log.d(TAG,"Error adding dtc err: "+error.message)
                                    if (error.error == RequestError.ERR_OFFLINE)
                                        addDtcPackageToPending(dtcPackage)

                                    mainHandler.post({callback?.onError(error)})
                                }

                            })
                        }

                    }
                    override fun onError(error: RequestError){
                        Log.d(TAG,"Error retrieving car err: "+error.message)
                        if (error.error == RequestError.ERR_OFFLINE)
                            addDtcPackageToPending(dtcPackage)
                        mainHandler.post({callback?.onError(error)})
                    }

                })

            }
            override fun onError(error: RequestError){
                Log.d(TAG,"Error retrieving user err: "+error.message)
                if (error.error == RequestError.ERR_OFFLINE)
                    addDtcPackageToPending(dtcPackage)
                mainHandler.post({callback?.onError(error)})
            }
        })
    }

    fun addDtcPackageToPending(dtc: DtcPackage){
        if (!PeriodicDtcSender.getInstance(this@AddDtcUseCaseImpl)
                .hasPendingDtcPakcage(dtcPackage as DtcPackage)){

            PeriodicDtcSender.getInstance(this@AddDtcUseCaseImpl)
                    .addPendingDtcPackage(dtcPackage as DtcPackage)
            Log.d(TAG,"Dtc package added to pending dtc: "+dtcPackage)
        }
    }

}