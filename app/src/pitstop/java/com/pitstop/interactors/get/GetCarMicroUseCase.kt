package com.pitstop.interactors.get

import com.pitstop.models.Car
import com.pitstop.models.Dealership
import com.pitstop.models.ObdScanner
import com.pitstop.models.Settings
import com.pitstop.network.RequestError
import com.pitstop.repositories.*
import com.pitstop.utils.ModelConverter

/**
 * Created by Karol Zdebel on 10/31/2017.
 */
class GetCarMicroUseCase(val userRepository: UserRepository, val carRepository: CarRepository
                         , val shopRepository: ShopRepository, val scannerRepository: ScannerRepository) {

    interface Callback{
        fun onGotCar(car: Car)
        fun onError(error: RequestError)
    }

    fun getUserCar(callback: Callback){

        userRepository.getCurrentUserSettings(object: Repository.Callback<Settings>{

            override fun onSuccess(settings: Settings) {

                carRepository.getShopId(settings.carId).subscribe({ shopIdResponse ->
                    scannerRepository.getScanner(settings.carId, object: Repository.Callback<ObdScanner>{

                        override fun onSuccess(obdScanner: ObdScanner?) {
                            shopRepository.get(shopIdResponse.response, object: Repository.Callback<Dealership>{

                                override fun onSuccess(dealership: Dealership) {
                                    carRepository.get(settings.carId)
                                            .subscribe({ carResponse ->
                                                var scannerId: String? = null
                                                if (obdScanner != null)
                                                    scannerId = obdScanner.scannerId
                                                val car = ModelConverter().generateCar(carResponse.response
                                                        , settings.carId, scannerId, dealership)
                                                callback.onGotCar(car)
                                            })
                                }

                                override fun onError(error: RequestError) {
                                    callback.onError(error)
                                }

                            })
                        }

                        override fun onError(error: RequestError) {
                            callback.onError(error)
                        }

                    })
                })
            }

            override fun onError(error: RequestError) {
                callback.onError(error)
            }
        })
    }
}