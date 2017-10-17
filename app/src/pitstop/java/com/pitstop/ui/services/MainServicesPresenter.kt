package com.pitstop.ui.services

import com.pitstop.BuildConfig
import com.pitstop.dependency.UseCaseComponent
import com.pitstop.interactors.get.GetUserCarUseCase
import com.pitstop.models.Car
import com.pitstop.network.RequestError
import com.pitstop.ui.services.upcoming.MainServicesView

/**
 * Created by Karol Zdebel on 10/17/2017.
 */
class MainServicesPresenter(val useCaseComponent: UseCaseComponent){

    var view: MainServicesView? = null

    fun subscribe(view: MainServicesView){
        this.view = view
    }

    fun unsubscribe(){
        this.view = null
    }

    fun populateUI() {
        useCaseComponent.userCarUseCase.execute(object : GetUserCarUseCase.Callback {

            override fun onCarRetrieved(car: Car) {

                //Update tab design to the current dealerships custom design if applicable
                if (car.dealership != null && view != null) {
                    if (BuildConfig.DEBUG && (car.dealership.id == 4 || car.dealership.id == 18)) {

                        view?.bindMercedesDealerUI()

                    } else if (!BuildConfig.DEBUG && car.dealership.id == 14) {

                        view?.bindMercedesDealerUI()

                    } else {
                        view?.bindDefaultDealerUI()
                    }
                }
            }

            override fun onNoCarSet() {

            }

            override fun onError(error: RequestError) {

            }
        })
    }
}