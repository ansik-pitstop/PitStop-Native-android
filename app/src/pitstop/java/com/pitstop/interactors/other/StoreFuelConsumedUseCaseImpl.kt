package com.pitstop.interactors.other

import android.os.Handler
import com.pitstop.database.LocalFuelConsumptionStorage
import com.pitstop.network.RequestError
import com.pitstop.repositories.Repository

/**
 * Created by ishan on 2017-11-14.
 */
class StoreFuelConsumedUseCaseImpl( val mainHandler:Handler, val useCaseHandler: Handler,
                                    val localFuelConsumptionStorage: LocalFuelConsumptionStorage ): StoreFuelConsumedUseCase {
    private var carId: Int = 0;
    private var callback: StoreFuelConsumedUseCase.Callback? = null
    private var fuelConsumed :Double = 0.0;

    override fun execute(carID: Int, fuelConsumed: Double, callback: StoreFuelConsumedUseCase.Callback) {
        this.carId = carID
        this.fuelConsumed = fuelConsumed;
        this.callback = callback;
        useCaseHandler.post(this)
    }

    override fun run() {

        localFuelConsumptionStorage.storeFuelConsumed(carId, fuelConsumed, object : Repository.Callback<Double>{
            override fun onSuccess(data: Double?) {
                mainHandler.post({callback?.onFuelConsumedStored(data!!)})
            }

            override fun onError(error: RequestError?) {
                mainHandler.post({callback?.onError(error!!)});
            }
        })
    }
}