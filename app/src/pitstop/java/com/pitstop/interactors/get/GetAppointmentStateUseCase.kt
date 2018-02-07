package com.pitstop.interactors.get

import com.pitstop.interactors.Interactor
import com.pitstop.models.Appointment
import com.pitstop.network.RequestError
import com.pitstop.retrofit.PredictedService

/**
 * Created by Karol Zdebel on 2/7/2018.
 */
interface GetAppointmentStateUseCase: Interactor {

    interface Callback{
        fun onPredictedServiceState(predictedService: PredictedService)
        fun onAppointmentBookedState(appointment: Appointment)
        fun onMileageUpdateNeededState()
        fun onError(error: RequestError)
    }

    fun execute(callback: Callback)
}