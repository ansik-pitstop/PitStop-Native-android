package com.pitstop.interactors.get

import android.os.Handler
import com.pitstop.models.Dealership
import com.pitstop.models.Settings
import com.pitstop.network.RequestError
import com.pitstop.repositories.CarRepository
import com.pitstop.repositories.Repository
import com.pitstop.repositories.ShopRepository
import com.pitstop.repositories.UserRepository

/**
 * Created by Karol Zdebel on 10/25/2017.
 */
class GetCurrentCarsDealershipUseCaseImpl(val userRepository: UserRepository, val carRepository: CarRepository
                                          , val shopRepository: ShopRepository, val useCaseHandler: Handler, val mainHandler: Handler)
    : GetCurrentCarsDealershipUseCase {

    var callback: GetCurrentCarsDealershipUseCase.Callback? = null

    override fun execute(callback: GetCurrentCarsDealershipUseCase.Callback) {
        this.callback = callback
        useCaseHandler.post(this)
    }

    override fun run() {
        userRepository.getCurrentUserSettings(object: Repository.Callback<Settings>{

            override fun onSuccess(settings: Settings) {

                if (!settings.hasMainCar()){
                    mainHandler.post({callback!!.onNoCarExists()})
                    return
                }

                carRepository.getShopId(settings.carId, object: Repository.Callback<Int>{
                    override fun onSuccess(shopId: Int) {
                        shopRepository.get(shopId, object: Repository.Callback<Dealership>{

                            override fun onSuccess(dealership: Dealership) {
                                mainHandler.post({callback!!.onGotDealership(dealership)})
                            }

                            override fun onError(error: RequestError) {
                                mainHandler.post({callback!!.onError(error)})
                            }
                        })
                    }

                    override fun onError(error: RequestError) {
                        mainHandler.post({callback!!.onError(error)})
                    }
                })
            }

            override fun onError(error: RequestError) {
                mainHandler.post({callback!!.onError(error)})
            }
        })
    }
}