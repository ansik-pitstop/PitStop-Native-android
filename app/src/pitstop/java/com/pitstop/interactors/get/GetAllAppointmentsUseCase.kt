package com.pitstop.interactors.get

import com.pitstop.interactors.Interactor
import com.pitstop.models.Appointment
import com.pitstop.network.RequestError

/**
 * Created by Karol Zdebel on 2/5/2018.
 */
interface GetAllAppointmentsUseCase: Interactor {

    fun execute(callback: Callback)

    interface Callback{
        fun onGotAppointments(appointments: List<Appointment>);
        fun onError(error: RequestError)
    }
}