package com.pitstop.interactors.get

import android.os.Handler
import com.pitstop.models.Car
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

    private var callback: GetDealershipWithCarIssuesUseCase.Callback? = null

    override fun execute(callback: GetDealershipWithCarIssuesUseCase.Callback) {
        this.callback = callback
        useCaseHandler.post(this)
    }

    override fun run() {

        userRepository.getCurrentUserSettings(object: Repository.Callback<Settings>{

            override fun onSuccess(settings: Settings) {
                carRepository.get(settings.carId, settings.userId, object: Repository.Callback<Car>{

                    override fun onSuccess(car: Car) {
                        carIssueRepository.getCurrentCarIssues(car.id, object: Repository.Callback<List<CarIssue>>{

                            override fun onSuccess(carIssueList: List<CarIssue>) {

                                shopRepository.get(car.shopId, settings.userId, object: Repository.Callback<Dealership>{

                                    override fun onSuccess(dealership: Dealership) {
                                        mainHandler.post({callback!!.onGotDealershipAndIssues(dealership, carIssueList)})
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

            override fun onError(error: RequestError) {
                mainHandler.post({callback!!.onError(error)})
            }
        })
    }
}