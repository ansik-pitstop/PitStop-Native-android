package com.pitstop.interactors.add

import android.os.Handler
import android.util.Log
import com.pitstop.bluetooth.dataPackages.DtcPackage
import com.pitstop.models.DebugMessage
import com.pitstop.models.Settings
import com.pitstop.network.RequestError
import com.pitstop.repositories.*
import com.pitstop.utils.Logger
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

/**
 * Created by Karol Zdebel on 10/11/2017.
 */
class AddDtcUseCaseImpl(val userRepository: UserRepository, val carIssueRepository: CarIssueRepository
                        , val carRepository: CarRepository, val useCaseHandler: Handler, val mainHandler: Handler) : AddDtcUseCase {

    private val tag = javaClass.simpleName
    private var dtcPackage: DtcPackage? = null
    private var callback: AddDtcUseCase.Callback? = null
    private var compositeDisposable = CompositeDisposable()
    private var carId: Int = 0

    override fun execute(carId: Int, dtcPackage: DtcPackage, callback: AddDtcUseCase.Callback) {
        Logger.getInstance()!!.logI(tag, "Use case execution started input: dtcPackage=" + dtcPackage
                , DebugMessage.TYPE_USE_CASE)
        this.dtcPackage = dtcPackage
        this.callback = callback
        this.carId = carId
        useCaseHandler.post(this)
    }

    private fun onError(error: RequestError){
        Logger.getInstance()!!.logE(tag, "Use case returned error: err=" + error
                , DebugMessage.TYPE_USE_CASE)
        compositeDisposable.clear()
        mainHandler.post({callback?.onError(error)})
    }

    private fun onDtcPackageAdded(dtcPackage: DtcPackage){
        Logger.getInstance()!!.logI(tag, "Use case execution finished: dtc package=" + dtcPackage
                , DebugMessage.TYPE_USE_CASE)
        compositeDisposable.clear()
        mainHandler.post({callback?.onDtcPackageAdded(dtcPackage)})
    }

    override fun run() {
        val disposable = carRepository.get(this.carId, Repository.DATABASE_TYPE.REMOTE)
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.io())
                .subscribe({response ->
                    if (response.data == null){
                        callback?.onError(RequestError.getUnknownError())
                        return@subscribe
                    }
                    for ((dtc, isPending) in dtcPackage!!.dtcs){
                        Log.d(tag,String.format("(dtc, isPending): (%s,%b)",dtc,isPending))
                        carIssueRepository.insertDtc(this.carId, response.data.totalMileage
                                , dtcPackage?.rtcTime!!.toLong(), dtc, isPending, object : Repository.Callback<String> {

                            override fun onSuccess(dtcCode: String){
                                Log.d(tag,"successfully added dtc code: "+dtcCode)
                                if (dtcPackage!!.dtcs.keys.indexOf(dtcCode) == dtcPackage!!.dtcs.keys.size-1){
                                    this@AddDtcUseCaseImpl.onDtcPackageAdded(dtcPackage as DtcPackage)
                                }

                            }
                            override fun onError(error: RequestError) {
                                Log.d(tag,"Error adding dtc err: "+error.message)
                                Log.d(tag,"dtcPackage: "+dtcPackage)
                                this@AddDtcUseCaseImpl.onError(error)
                            }
                        })
                    }
                }, {err -> this@AddDtcUseCaseImpl.onError(RequestError(err)) })
        compositeDisposable.add(disposable)
    }
}