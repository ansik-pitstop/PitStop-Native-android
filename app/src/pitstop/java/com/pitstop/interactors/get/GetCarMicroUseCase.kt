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
                carRepository.get(settings.carId)
                        .doOnError({err -> callback.onError(RequestError.getUnknownError())})
                        .subscribe({carResponse ->
                            carRepository.getShopId(carResponse.response._id)
                                    .doOnError({err -> callback.onError(RequestError.getUnknownError())})
                                    .subscribe({shopIdResponse ->
                                        shopRepository.get(shopIdResponse.response, object: Repository.Callback<Dealership>{

                                            override fun onSuccess(dealership: Dealership) {
                                                scannerRepository.getScanner(settings.carId, object: Repository.Callback<ObdScanner>{

                                                    override fun onSuccess(scanner: ObdScanner) {
                                                        callback.onGotCar(ModelConverter()
                                                                .generateCar(carResponse.response
                                                                        , settings.carId, scanner.scannerId
                                                                        , dealership))
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
                })
            }

            override fun onError(error: RequestError) {
                callback.onError(error)
            }
        })
    }
}