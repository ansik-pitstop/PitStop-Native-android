package com.pitstop.interactors.add

import android.os.Handler
import com.pitstop.repositories.CarIssueRepository
import com.pitstop.repositories.UserRepository

/**
 * Created by Karol Zdebel on 10/11/2017.
 */
class AddDtcUseCaseImpl(userRepository: UserRepository, carIssueRepository: CarIssueRepository
                        , useCaseHandler: Handler, mainHandler: Handler) : AddDtcUseCase {

    var dtc: String? = null
    var isPending: Boolean? = null
    var rtcTime: Long? = null
    var callback: AddDtcUseCase.Callback? = null
    var useCaseHandler: Handler? = null
    var mainHandler: Handler? = null

    override fun execute(dtc: String, isPending: Boolean, rtcTime: Long
                         , callback: AddDtcUseCase.Callback) {
        this.dtc = dtc
        this.isPending = isPending
        this.rtcTime = rtcTime
        this.callback = callback
    }

    override fun run() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}