package com.pitstop.interactors.get

import android.os.Handler
import android.util.Log
import com.pitstop.models.Dealership
import com.pitstop.models.DebugMessage
import com.pitstop.models.Settings
import com.pitstop.network.RequestError
import com.pitstop.repositories.*
import com.pitstop.utils.Logger

/**
 * Created by Karol Zdebel on 10/25/2017.
 */
class GetCurrentCarDealershipUseCaseImpl(val userRepository: UserRepository, val carRepository: CarRepository
                                         , val shopRepository: ShopRepository, val useCaseHandler: Handler, val mainHandler: Handler)
    : GetCurrentCarDealershipUseCase {

    val tag: String? = javaClass.simpleName
    private var callback: GetCurrentCarDealershipUseCase.Callback? = null
    private var carId: Int = 0

    override fun execute(carId: Int, callback: GetCurrentCarDealershipUseCase.Callback) {
        Logger.getInstance()!!.logI(tag, "Use case execution started", DebugMessage.TYPE_USE_CASE)
        this.callback = callback
        this.carId = carId
        useCaseHandler.post(this)
    }

    private fun onError(error:RequestError){
        Logger.getInstance()!!.logE(tag, "Use case returned error: err="+error
                , DebugMessage.TYPE_USE_CASE)
        mainHandler.post({callback!!.onError(error)})
    }

    private fun onGotDealership(dealership: Dealership){
        Logger.getInstance()!!.logI(tag, "Use case finished: dealership="+dealership
                , DebugMessage.TYPE_USE_CASE)
        mainHandler.post({callback!!.onGotDealership(dealership)})
    }

    private fun onNoCarExists(){
        Logger.getInstance()!!.logI(tag, "Use case finished: no car exists!"
                , DebugMessage.TYPE_USE_CASE)
        mainHandler.post({callback!!.onNoCarExists()})
    }

    override fun run() {
        carRepository.getShopId(this.carId, object: Repository.Callback<Int>{
            override fun onSuccess(shopId: Int) {
                Log.d(tag,"got shop id: $shopId")
                shopRepository.get(shopId, object: Repository.Callback<Dealership>{

                    override fun onSuccess(dealership: Dealership) {
                        Log.d(tag,"got dealership: $dealership")
                        this@GetCurrentCarDealershipUseCaseImpl.onGotDealership(dealership)
                    }

                    override fun onError(error: RequestError) {
                        Log.d(tag,"error getting dealership: ${error.message}")
                        this@GetCurrentCarDealershipUseCaseImpl.onError(error)
                    }
                })
            }

            override fun onError(error: RequestError) {
                Log.d(tag,"error getting shop id: ${error.message}")
                this@GetCurrentCarDealershipUseCaseImpl.onError(error)
            }
        })
    }
}