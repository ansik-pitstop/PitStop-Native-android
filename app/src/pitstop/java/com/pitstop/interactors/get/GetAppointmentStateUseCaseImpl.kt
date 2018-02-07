package com.pitstop.interactors.get

import android.os.Handler
import com.pitstop.models.Appointment
import com.pitstop.models.DebugMessage
import com.pitstop.network.RequestError
import com.pitstop.repositories.AppointmentRepository
import com.pitstop.repositories.UserRepository
import com.pitstop.retrofit.PredictedService
import com.pitstop.utils.Logger

/**
 * Created by Karol Zdebel on 2/7/2018.
 */
class GetAppointmentStateUseCaseImpl(private val userRepository: UserRepository
                                     , private val appointmentRepository: AppointmentRepository
                                     , private val usecaseHandler: Handler
                                     , private val mainHandler: Handler)
                                     : GetAppointmentStateUseCase {

    private val tag = javaClass.simpleName
    private var callback: GetAppointmentStateUseCase.Callback? = null

    override fun execute(callback: GetAppointmentStateUseCase.Callback) {
        Logger.getInstance()!!.logI(tag, "Use case started execution"
                , DebugMessage.TYPE_USE_CASE)
        this.callback = callback
        usecaseHandler.post(this)
    }

    override fun run() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun onMileageUpdateNeededState(){
        Logger.getInstance()!!.logI(tag, "Use case finished: onMileageUdateNeededState"
                , DebugMessage.TYPE_USE_CASE)
        mainHandler.post({callback!!.onMileageUpdateNeededState()})
    }

    private fun onAppointmentBookedState(appointment: Appointment){
        Logger.getInstance()!!.logI(tag, "Use case finished: onAppointmentBookedState: "
                +appointment, DebugMessage.TYPE_USE_CASE)
        mainHandler.post({callback!!.onAppointmentBookedState(appointment)})
    }

    private fun onPredictedServiceState(predictedService: PredictedService){
        Logger.getInstance()!!.logI(tag, "Use case finished: onPredictedServiceState: "
                +predictedService, DebugMessage.TYPE_USE_CASE)
        mainHandler.post({callback!!.onPredictedServiceState(predictedService)})
    }

    private fun onError(requestError: RequestError?){
        val error: RequestError = if (requestError != null) requestError else RequestError.getUnknownError()
        Logger.getInstance()!!.logE(tag, "Use case returned error: err="
                + error, DebugMessage.TYPE_USE_CASE)
        mainHandler.post({callback!!.onError(error)})
    }
}