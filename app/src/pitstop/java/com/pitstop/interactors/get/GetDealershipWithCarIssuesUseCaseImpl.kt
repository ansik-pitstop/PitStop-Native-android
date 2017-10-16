package com.pitstop.interactors.get

import android.os.Handler
import com.pitstop.repositories.CarRepository
import com.pitstop.repositories.UserRepository

/**
 * Created by Karol Zdebel on 10/16/2017.
 */
class GetDealershipWithCarIssuesUseCaseImpl(val userRepository: UserRepository
        , val carRepository: CarRepository, val useCaseHandler: Handler, val mainHandler: Handler)
    : GetDealershipWithCarIssuesUseCase {

    private var callback: GetDealershipWithCarIssuesUseCase.Callback? = null

    override fun execute(callback: GetDealershipWithCarIssuesUseCase.Callback) {
        this.callback = callback
        useCaseHandler.post(this)
    }

    override fun run() {

    }
}