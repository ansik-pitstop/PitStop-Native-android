package com.pitstop.interactors.get

import android.os.Handler
import android.util.Log
import com.pitstop.models.Car
import com.pitstop.models.Dealership
import com.pitstop.models.Settings
import com.pitstop.models.User
import com.pitstop.network.RequestError
import com.pitstop.repositories.*
import io.reactivex.schedulers.Schedulers

/**
 * Created by Karol Zdebel on 10/12/2017.
 */
class GetCarsWithDealershipsUseCaseImpl(val userRepository: UserRepository
                                        , val carRepository: CarRepository, val shopRepository: ShopRepository
                                        , val useCaseHandler: Handler, val mainHandler: Handler)
    : GetCarsWithDealershipsUseCase {

    val tag: String? = javaClass.simpleName
    var callback: GetCarsWithDealershipsUseCase.Callback? = null

    override fun execute(callback: GetCarsWithDealershipsUseCase.Callback) {
        this.callback = callback
        useCaseHandler.post(this)
    }

    override fun run() {
        val map = LinkedHashMap<Car,Dealership>()
        userRepository.getCurrentUser(object : Repository.Callback<User> {

            override fun onSuccess(user: User) {
                carRepository.getCarsByUserId(user.id)
                        .doOnNext { carListResponse ->
                            Log.d(tag, "getCarsByUserId() response: " + carListResponse)
                            val carList = carListResponse.data
                            if (carList == null) {
                                mainHandler.post({onError(RequestError.getUnknownError())})
                                return@doOnNext
                            }
                            userRepository.getCurrentUserSettings(object: Repository.Callback<Settings>{
                                override fun onSuccess(settings: Settings) {
                                    if (carList.isEmpty()) mainHandler.post({callback!!.onGotCarsWithDealerships(map)})
                                    for (c in carList){
                                        c.isCurrentCar = c.id == settings.carId
                                        shopRepository.getAllShops(object : Repository.Callback<List<Dealership>>{

                                            override fun onSuccess(dealershipList: List<Dealership>) {
                                                Log.d(tag,"cars for user: "+carList)
                                                Log.d(tag,"shops for user: "+dealershipList)
                                                for (car in carList){
                                                    dealershipList
                                                            .filter { car.shopId == it.id }
                                                            .forEach { map.put(car, it) }
                                                }
                                                Log.d(tag,"Resulting map: "+map)
                                                mainHandler.post({callback!!.onGotCarsWithDealerships(map)})
                                            }

                                            override fun onError(error: RequestError) {
                                                mainHandler.post({callback!!.onError(error)})
                                            }
                                        })
                                    }
                                }

                                override fun onError(error: RequestError) {
                                    mainHandler.post({callback!!.onError(error)})
                                }
                            })

                        }.onErrorReturn { err ->
                    Log.d(tag, "getCarsByUserId() err: " + err)
                    Response<List<Car>>(null, false)
                }.subscribeOn(Schedulers.io())
                        .subscribe()
            }

            override fun onError(error: RequestError) {
                mainHandler.post({callback!!.onError(error)})
            }
        })
    }
}