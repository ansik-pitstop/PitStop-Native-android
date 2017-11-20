package com.pitstop.interactors.get

import android.os.Handler
import android.util.Log
import com.pitstop.models.*
import com.pitstop.network.RequestError
import com.pitstop.repositories.*
import com.pitstop.utils.Logger
import io.reactivex.android.schedulers.AndroidSchedulers
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
        Logger.getInstance()!!.logI(tag, "Use case execution started", false, DebugMessage.TYPE_USE_CASE)
        this.callback = callback
        useCaseHandler.post(this)
    }

    fun onGotCarsWithDealerships(data: LinkedHashMap<Car,Dealership>, local: Boolean){
        Logger.getInstance()!!.logI(tag, "Use case finished: map="+data+", local="+local
                , false, DebugMessage.TYPE_USE_CASE)
        mainHandler.post({ callback!!.onGotCarsWithDealerships(data,local) })
    }

    fun onError(error: RequestError){
        Logger.getInstance()!!.logE(tag, "Use case returned error: err="+error, false, DebugMessage.TYPE_USE_CASE)
        mainHandler.post({onError(error)})
    }

    override fun run() {
        userRepository.getCurrentUser(object : Repository.Callback<User> {

            override fun onSuccess(user: User) {
                carRepository.getCarsByUserId(user.id)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.from(useCaseHandler.getLooper()))
                        .doOnError({err ->
                            Log.d(tag,"err: "+err)
                            this@GetCarsWithDealershipsUseCaseImpl.onError(RequestError(err))
                        })
                        .doOnNext { carListResponse ->
                            val carList = carListResponse.data
                            if (carList == null) {
                                this@GetCarsWithDealershipsUseCaseImpl.onError(com.pitstop.network.RequestError.getUnknownError())
                                return@doOnNext
                            }
                            userRepository.getCurrentUserSettings(object: Repository.Callback<Settings>{
                                override fun onSuccess(settings: Settings) {
                                    if (carList.isEmpty()) {
                                        this@GetCarsWithDealershipsUseCaseImpl.onGotCarsWithDealerships(LinkedHashMap<Car,Dealership>()
                                                ,carListResponse.isLocal)
                                        return@onSuccess
                                    }

                                    shopRepository.getAllShops(object : Repository.Callback<List<Dealership>> {
                                        override fun onSuccess(dealershipList: List<Dealership>) {
                                            val map = LinkedHashMap<Car,Dealership>()
                                            for (c in carList) {
                                                c.isCurrentCar = c.id == settings.carId
                                                dealershipList
                                                    .filter { c.shopId == it.id }
                                                    .forEach {
                                                        c.shop = it
                                                        map.put(c, it)
                                                    }
                                            }
                                            this@GetCarsWithDealershipsUseCaseImpl.onGotCarsWithDealerships(map,carListResponse.isLocal)
                                        }

                                        override fun onError(error: RequestError) {
                                            this@GetCarsWithDealershipsUseCaseImpl.onError(error)
                                        }
                                    })
                                }

                                override fun onError(error: RequestError) {
                                    this@GetCarsWithDealershipsUseCaseImpl.onError(error)
                                }

                            })

                        }.onErrorReturn { err ->
                            RepositoryResponse<List<Car>>(null, false)
                        }.doOnError({err -> this@GetCarsWithDealershipsUseCaseImpl.onError(RequestError(err))})
                        .subscribe()
            }

            override fun onError(error: RequestError) {
                this@GetCarsWithDealershipsUseCaseImpl.onError(error)
            }
        })
    }
}