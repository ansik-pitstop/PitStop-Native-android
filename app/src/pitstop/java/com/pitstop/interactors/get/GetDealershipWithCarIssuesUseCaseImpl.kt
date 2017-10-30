package com.pitstop.interactors.get

import android.os.Handler
import android.util.Log
import com.pitstop.models.Dealership
import com.pitstop.models.Settings
import com.pitstop.models.issue.CarIssue
import com.pitstop.network.RequestError
import com.pitstop.repositories.*

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
        this.callback = callback
        useCaseHandler.post(this)
    }

    override fun run() {
        Log.d(tag,"run()")
        userRepository.getCurrentUserSettings(object: Repository.Callback<Settings>{

            override fun onSuccess(settings: Settings) {
                Log.d(tag, "got settings")

                carIssueRepository.getCurrentCarIssues(settings.carId, object : Repository.Callback<List<CarIssue>> {

                    override fun onSuccess(carIssueList: List<CarIssue>) {
                        carRepository.getShopId(settings.carId, object: Repository.Callback<Int>{
                            override fun onSuccess(shopId: Int) {
                                shopRepository.get(shopId, object : Repository.Callback<Dealership> {

                                    override fun onSuccess(dealership: Dealership) {
                                        Log.d(tag, "got dealership, callback.onSuccess()")
                                        mainHandler.post({ callback!!.onGotDealershipAndIssues(dealership, carIssueList) })
                                    }

                                    override fun onError(error: RequestError) {
                                        Log.d(tag, "onError() err: ${error.message}")
                                        mainHandler.post({ callback!!.onError(error) })
                                    }
                                })
                            }

                            override fun onError(error: RequestError) {
                                callback!!.onError(error)
                            }
                        })
                        Log.d(tag, "got car issues")

                    }

                    override fun onError(error: RequestError) {
                        mainHandler.post({ callback!!.onError(error) })
                    }

                })
            }
            override fun onError(error: RequestError) {
                Log.d(tag,"onError() err: ${error.message}")
                mainHandler.post({callback!!.onError(error)})
            }

        })
    }
}