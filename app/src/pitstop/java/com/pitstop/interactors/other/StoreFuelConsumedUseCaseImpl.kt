package com.pitstop.interactors.other

import android.os.Handler
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

    override fun execute(fuelConsumed: Double, callback: StoreFuelConsumedUseCase.Callback) {

        this.fuelConsumed = fuelConsumed;
        this.callback = callback;
        useCaseHandler.post(this)
    }

    override fun run() {

        userRepository.getCurrentUserSettings(object : Repository.Callback<Settings>{

            override fun onSuccess(data: Settings?) {
                localFuelConsumptionStorage.storeFuelConsumed(data!!.carId, fuelConsumed, object : Repository.Callback<Double>{
                    override fun onSuccess(data: Double?) {
                        mainHandler.post({callback?.onFuelConsumedStored(data!!)})
                    }

                    override fun onError(error: RequestError?) {
                        mainHandler.post({callback?.onError(error!!)});
                    }
                })
            }

            override fun onError(error: RequestError?) {


            }
        })
    }
}