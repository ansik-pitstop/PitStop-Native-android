package com.pitstop.interactors.add

import com.pitstop.bluetooth.dataPackages.DtcPackage
import com.pitstop.interactors.Interactor
import com.pitstop.network.RequestError

/**
 * Created by Karol Zdebel on 10/11/2017.
 */
interface AddDtcUseCase: Interactor {

    interface Callback{
        fun onDtcPackageAdded(dtc: DtcPackage)
        fun onError(requestError: RequestError)
    }

    fun execute(dtcPackage: DtcPackage, callback: Callback)
}