package com.pitstop.interactors.get

import android.os.Handler
import com.pitstop.database.LocalAlarmStorage
import com.pitstop.interactors.add.AddAlarmUseCase
import com.pitstop.repositories.CarRepository
import com.pitstop.repositories.UserRepository

/**
 * Created by ishan on 2017-10-30.
 */

class GetAlarmsUseCaseImpl(val userRepository: UserRepository, val carRepository: CarRepository, val localAlarmStorage: LocalAlarmStorage,
                           val useCaseHandler: Handler, val mainHandler: Handler): GetAlarmsUseCase {

    private val TAG = javaClass.simpleName;
    private var callback: GetAlarmsUseCase.Callback? = null
    private var carId: Int? = null



    override fun execute(carID: Int, callback: GetAlarmsUseCase.Callback) {
        this.carId = carID
        this.callback = callback
        useCaseHandler.post(this)
    }

    override fun run() {

    }


}