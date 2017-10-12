package com.pitstop.interactors.get

import android.os.Handler
import android.util.Log
import com.pitstop.models.Car
import com.pitstop.models.Dealership
import com.pitstop.models.User
import com.pitstop.network.RequestError
import com.pitstop.repositories.CarRepository
import com.pitstop.repositories.Repository
import com.pitstop.repositories.ShopRepository
import com.pitstop.repositories.UserRepository

/**
 * Created by Karol Zdebel on 10/12/2017.
 */
class GetCarsWithDealershipsUseCaseImpl(val userRepository: UserRepository
                                        , val carRepository: CarRepository, val shopRepository: ShopRepository
                                        , val useCaseHandler: Handler, val mainHandler: Handler)
    : GetCarsWithDealershipsUseCase {

    val tag = javaClass.simpleName
    var callback: GetCarsWithDealershipsUseCase.Callback? = null

    override fun execute(callback: GetCarsWithDealershipsUseCase.Callback) {
        this.callback = callback
        useCaseHandler.post(this)
    }

    override fun run() {
        val map = HashMap<Car,Dealership>()
        userRepository.getCurrentUser(object : Repository.Callback<User> {

            override fun onSuccess(user: User) {

                carRepository.getCarsByUserId(user.id, object : Repository.Callback<List<Car>>{

                    override fun onSuccess(carList: List<Car>) {

                        shopRepository.getShopsByUserId(user.id,object : Repository.Callback<List<Dealership>>{

                            override fun onSuccess(dealershipList: List<Dealership>) {
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