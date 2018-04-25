package com.pitstop.interactors.add

import com.pitstop.bluetooth.dataPackages.PidPackage
import com.pitstop.interactors.Interactor
import com.pitstop.network.RequestError

/**
 * Created by Karol Zdebel on 4/24/2018.
 */
interface AddPidUseCase: Interactor {
    interface Callback{
        fun onAdded(size: Int)
        fun onError(error: RequestError)
    }

    fun execute(pidPackage: PidPackage, vin: String, callback: Callback)
}