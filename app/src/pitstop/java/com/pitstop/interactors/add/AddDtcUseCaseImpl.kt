package com.pitstop.interactors.add

import android.os.Handler
import android.util.Log
import com.pitstop.bluetooth.dataPackages.DtcPackage
import com.pitstop.models.Settings
import com.pitstop.network.RequestError
import com.pitstop.repositories.*
import io.reactivex.schedulers.Schedulers

/**
 * Created by Karol Zdebel on 10/11/2017.
 */
class AddDtcUseCaseImpl(val userRepository: UserRepository, val carIssueRepository: CarIssueRepository
                        , val carRepository: CarRepository, val useCaseHandler: Handler, val mainHandler: Handler) : AddDtcUseCase {

    private val tag = javaClass.simpleName
    private var dtcPackage: DtcPackage? = null
    private var callback: AddDtcUseCase.Callback? = null

    override fun execute(dtcPackage: DtcPackage, callback: AddDtcUseCase.Callback) {
        Log.d(tag,"execute() dtcPackage: "+dtcPackage)
        this.dtcPackage = dtcPackage
        this.callback = callback
        useCaseHandler.post(this)
    }

    override fun run() {
        userRepository.getCurrentUserSettings(object : Repository.Callback<Settings> {

            override fun onSuccess(settings: Settings){

                if (!settings.hasMainCar()){
                    callback?.onError(RequestError.getUnknownError())
                    return
                }

                carRepository.get(settings.carId).doOnNext({response ->
                    if (response.data == null){
                        callback?.onError(RequestError.getUnknownError())
                        return@doOnNext
                    }
                    for ((dtc, isPending) in dtcPackage!!.dtcs){
                        Log.d(tag,String.format("(dtc, isPending): (%s,%b)",dtc,isPending))
                        carIssueRepository.insertDtc(settings.carId, response.data.totalMileage
                                , dtcPackage?.rtcTime!!.toLong(), dtc, isPending, object : Repository.Callback<String> {

                            override fun onSuccess(dtcCode: String){
                                Log.d(tag,"successfully added dtc code: "+dtcCode)
                                if (dtcPackage!!.dtcs.keys.indexOf(dtcCode) == dtcPackage!!.dtcs.keys.size-1){
                                    mainHandler.post({callback?.onDtcPackageAdded(dtcPackage as DtcPackage)})
                                    Log.d(tag,"Added entire DtcPackage");
                                }

                            }
                            override fun onError(error: RequestError){
                                Log.d(tag,"Error adding dtc err: "+error.message)
                                Log.d(tag,"dtcPackage: "+dtcPackage)
                                mainHandler.post({callback?.onError(error)})
                            }

                        })
                    }
                }).onErrorReturn({err ->
                    Log.d(tag,"Error retrieving car err: "+err.message)
                    RepositoryResponse(null,false)
                }).subscribeOn(Schedulers.io())
                .subscribe()
            }
            override fun onError(error: RequestError){
                Log.d(tag,"Error retrieving user err: "+error.message)
                mainHandler.post({callback?.onError(error)})
            }
        })
    }

}