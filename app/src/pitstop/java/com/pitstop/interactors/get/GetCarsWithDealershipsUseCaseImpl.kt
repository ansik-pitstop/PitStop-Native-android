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
        userRepository.getCurrentUser(object : Repository.Callback<User> {

            override fun onSuccess(user: User) {
                carRepository.getCarsByUserId(user.id)
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.computation())
                        .doOnNext { carListResponse ->
                            Log.d(tag, "getCarsByUserId() response: " + carListResponse)
                            val carList = carListResponse.data
                            if (carList == null) {
                                mainHandler.post({onError(RequestError.getUnknownError())})
                                return@doOnNext
                            }
                            userRepository.getCurrentUserSettings(object: Repository.Callback<Settings>{
                                override fun onSuccess(settings: Settings) {
                                    Log.d(tag, "getCurrentUserSetting() resonse: " + settings)
                                    if (carList.isEmpty()) {
                                        mainHandler.post({ callback!!.onGotCarsWithDealerships(LinkedHashMap<Car,Dealership>()) })
                                        return@onSuccess
                                    }

                                    shopRepository.getAllShops(object : Repository.Callback<List<Dealership>> {
                                        override fun onSuccess(dealershipList: List<Dealership>) {
                                            val map = LinkedHashMap<Car,Dealership>()
                                            for (c in carList) {
                                                c.isCurrentCar = c.id == settings.carId
                                                Log.d(tag, "getAllShops() response: " + dealershipList)
                                                Log.d(tag, "cars for user: " + carList)
                                                Log.d(tag, "shops for user: " + dealershipList)
                                                dealershipList
                                                    .filter { c.shopId == it.id }
                                                    .forEach {
                                                        c.shop = it
                                                        map.put(c, it)
                                                    }
                                            }
                                            Log.d(tag, "Resulting map: " + map)
                                            mainHandler.post({ callback!!.onGotCarsWithDealerships(map) })
                                        }

                                        override fun onError(error: RequestError) {
                                            mainHandler.post({ callback!!.onError(error) })
                                        }
                                    })
                                }

                                override fun onError(error: RequestError) {
                                    mainHandler.post({callback!!.onError(error)})
                                }

                            })

                        }.onErrorReturn { err ->
                    Log.d(tag, "getCarsByUserId() err: " + err)
                    RepositoryResponse<List<Car>>(null, false)
                }
                .subscribe()
            }

            override fun onError(error: RequestError) {
                mainHandler.post({callback!!.onError(error)})
            }
        })
    }
}