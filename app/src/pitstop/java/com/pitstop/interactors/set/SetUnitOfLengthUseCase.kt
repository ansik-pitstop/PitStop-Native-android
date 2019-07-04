package com.pitstop.interactors.set

import com.pitstop.interactors.Interactor
import com.pitstop.network.RequestError
import com.pitstop.utils.UnitOfLength

interface SetUnitOfLengthUseCase: Interactor {

    interface Callback {
        fun onUnitSet(unit: UnitOfLength)
        fun onError(error: RequestError)
    }

    fun execute(unitOfLength: UnitOfLength, callback: Callback)
}