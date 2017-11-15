package com.pitstop.interactors.get

import android.os.Handler
import com.pitstop.database.LocalFuelConsumptionStorage
import com.pitstop.network.RequestError
import com.pitstop.repositories.Repository


/**
 * Created by ishan on 2017-11-14.
 */
class GetFuelConsumedUseCaseImpl (val mainHandler: Handler, val useCaseHandler: Handler,
                                  val localFuelConsumptionStorage: LocalFuelConsumptionStorage): GetFuelConsumedUseCase{
    private var scannerID: String? = null;
    private var callback: GetFuelConsumedUseCase.Callback? = null


    override fun execute(scanner: String, callback: GetFuelConsumedUseCase.Callback) {
       this.scannerID = scanner;
        this.callback = callback
        useCaseHandler.post(this)
    }

    override fun run() {
        localFuelConsumptionStorage.getFuelConsumed(scannerID, object : Repository.Callback<Double> {
            override fun onSuccess(data: Double?) {
                mainHandler.post({callback?.onFuelConsumedGot(data!!)})
            }

            override fun onError(error: RequestError?) {
                mainHandler.post({onError(error)})
            }
        })
    }
}