package com.pitstop.interactors.get

import android.os.Handler
import android.util.Log
import com.pitstop.models.Dealership
import com.pitstop.models.DebugMessage
import com.pitstop.models.Settings
import com.pitstop.models.issue.CarIssue
import com.pitstop.network.RequestError
import com.pitstop.repositories.*
import com.pitstop.utils.Logger

/**
 * Created by Karol Zdebel on 10/16/2017.
 */
class GetDealershipWithCarIssuesUseCaseImpl(val userRepository: UserRepository
        , val carRepository: CarRepository, val carIssueRepository: CarIssueRepository
                                            , val shopRepository: ShopRepository
                                            , val useCaseHandler: Handler, val mainHandler: Handler)
    : GetDealershipWithCarIssuesUseCase {

    private val tag = javaClass.simpleName
    private var callback: GetDealershipWithCarIssuesUseCase.Callback? = null

    override fun execute(callback: GetDealershipWithCarIssuesUseCase.Callback) {
        Logger.getInstance()!!.logI(tag, "Use case execution started"
                , DebugMessage.TYPE_USE_CASE)
        this.callback = callback
        useCaseHandler.post(this)
    }

    private fun onError(error: RequestError){
        Logger.getInstance()!!.logE(tag, "Use case returned error: err="+error
                , DebugMessage.TYPE_USE_CASE)
        callback!!.onError(error)
    }

    private fun onGotDealershipAndIssues(dealership: Dealership, carIssues: List<CarIssue>){
        Logger.getInstance()!!.logI(tag, "Use case finished: dealership=$dealership, carIssues=$carIssues"
                , DebugMessage.TYPE_USE_CASE)
        mainHandler.post({ callback!!.onGotDealershipAndIssues(dealership, carIssues) })
    }

    override fun run() {
        Log.d(tag,"run()")
        userRepository.getCurrentUserSettings(object: Repository.Callback<Settings>{

            override fun onSuccess(settings: Settings) {
                Log.d(tag, "got settings: "+settings)

                carIssueRepository.getCurrentCarIssues(settings.carId, object : Repository.Callback<List<CarIssue>> {

                    override fun onSuccess(carIssueList: List<CarIssue>) {
                        carRepository.getShopId(settings.carId, object: Repository.Callback<Int>{
                            override fun onSuccess(shopId: Int) {
                                shopRepository.get(shopId, object : Repository.Callback<Dealership> {

                                    override fun onSuccess(dealership: Dealership) {
                                        Log.d(tag, "got dealership: "+dealership)
                                        this@GetDealershipWithCarIssuesUseCaseImpl.onGotDealershipAndIssues(dealership,carIssueList)
                                    }

                                    override fun onError(error: RequestError) {
                                        Log.d(tag, "onError() err: ${error.message}")
                                        this@GetDealershipWithCarIssuesUseCaseImpl.onError(error)
                                    }
                                })
                            }

                            override fun onError(error: RequestError) {
                                this@GetDealershipWithCarIssuesUseCaseImpl.onError(error)
                            }
                        })
                        Log.d(tag, "got car issues")

                    }

                    override fun onError(error: RequestError) {
                        this@GetDealershipWithCarIssuesUseCaseImpl.onError(error)
                    }

                })
            }
            override fun onError(error: RequestError) {
                Log.d(tag,"onError() err: ${error.message}")
                this@GetDealershipWithCarIssuesUseCaseImpl.onError(error)
            }

        })
    }
}