package com.pitstop.interactors.other

import android.os.Handler
import android.util.Log
import com.pitstop.database.LocalFuelConsumptionStorage
import com.pitstop.models.Settings
import com.pitstop.network.RequestError
import com.pitstop.repositories.Repository
import com.pitstop.repositories.UserRepository

/**
 * Created by ishan on 2017-11-14.
 */
class StoreFuelConsumedUseCaseImpl(val  userRepository: UserRepository,  val mainHandler:Handler, val useCaseHandler: Handler,
                                    val localFuelConsumptionStorage: LocalFuelConsumptionStorage ): StoreFuelConsumedUseCase {
    private var callback: StoreFuelConsumedUseCase.Callback? = null
    private var fuelConsumed :Double = 0.0;
    private var scannerID: String? = null;
    private var TAG: String = StoreFuelConsumedUseCaseImpl::class.java.simpleName;

    override fun execute(id: String, fuelConsumed: Double, callback: StoreFuelConsumedUseCase.Callback) {
        this.scannerID = id
        this.fuelConsumed = fuelConsumed;
        this.callback = callback;
        useCaseHandler.post(this)
    }

    override fun run() {
        localFuelConsumptionStorage.storeFuelConsumed(scannerID, fuelConsumed, object : Repository.Callback<Double>{
            override fun onSuccess(data: Double?) {
                Log.d(TAG, "myScannerId is " + scannerID)
                    mainHandler.post({callback?.onFuelConsumedStored(data!!)})
            }

            override fun onError(error: RequestError?) {
                mainHandler.post({callback?.onError(error!!)});
            }
        })

    }
}