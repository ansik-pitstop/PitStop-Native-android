package com.pitstop.interactors.get

import android.os.Handler
import com.pitstop.database.LocalFuelConsumptionStorage
import com.pitstop.models.DebugMessage
import com.pitstop.network.RequestError
import com.pitstop.repositories.Repository
import com.pitstop.utils.Logger


/**
 * Created by ishan on 2017-11-14.
 */
class GetFuelConsumedUseCaseImpl (val mainHandler: Handler, val useCaseHandler: Handler,
                                  val localFuelConsumptionStorage: LocalFuelConsumptionStorage): GetFuelConsumedUseCase{
    private val tag:String = GetFuelConsumedUseCaseImpl::javaClass.name
    private var scannerID: String? = null;
    private var callback: GetFuelConsumedUseCase.Callback? = null


    override fun execute(scanner: String, callback: GetFuelConsumedUseCase.Callback) {
       this.scannerID = scanner;
        this.callback = callback
        useCaseHandler.post(this)
    }

    private fun onSuccess(fuelConseumed: Double){

        Logger.getInstance().logI(tag, "useCase success, fuelConsumed = $fuelConseumed", DebugMessage.TYPE_USE_CASE)
        mainHandler.post({callback?.onFuelConsumedGot(fuelConseumed)});

    }
    private fun onError(error: RequestError){
        Logger.getInstance().logE(tag, "useCase failed, error = ${error.message}", DebugMessage.TYPE_USE_CASE)
        mainHandler.post({callback?.onError(error)});

    }

    override fun run() {
        localFuelConsumptionStorage.getFuelConsumed(scannerID, object : Repository.Callback<Double> {
            override fun onSuccess(data: Double?) {
               this@GetFuelConsumedUseCaseImpl.onSuccess(data!!)
            }

            override fun onError(error: RequestError?) {
                this@GetFuelConsumedUseCaseImpl.onError(error!!)

            }
        })
    }
}