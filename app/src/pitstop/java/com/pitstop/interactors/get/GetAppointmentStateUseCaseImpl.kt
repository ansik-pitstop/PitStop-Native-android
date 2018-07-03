package com.pitstop.interactors.get

import android.os.Handler
import android.util.Log
import com.pitstop.models.Appointment
import com.pitstop.models.Dealership
import com.pitstop.models.DebugMessage
import com.pitstop.models.Settings
import com.pitstop.network.RequestError
import com.pitstop.repositories.AppointmentRepository
import com.pitstop.repositories.Repository
import com.pitstop.repositories.ShopRepository
import com.pitstop.repositories.UserRepository
import com.pitstop.retrofit.PredictedService
import com.pitstop.utils.Logger
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.*

/**
 * Created by Karol Zdebel on 2/7/2018.
 */
class GetAppointmentStateUseCaseImpl(private val userRepository: UserRepository
                                     , private val appointmentRepository: AppointmentRepository
                                     , private val shopRepository: ShopRepository
                                     , private val usecaseHandler: Handler
                                     , private val mainHandler: Handler)
                                     : GetAppointmentStateUseCase {

    private val tag = javaClass.simpleName
    private var callback: GetAppointmentStateUseCase.Callback? = null
    private var compositeDisposable = CompositeDisposable()

    override fun execute(callback: GetAppointmentStateUseCase.Callback) {
        Logger.getInstance()!!.logI(tag, "Use case started execution"
                , DebugMessage.TYPE_USE_CASE)
        this.callback = callback
        usecaseHandler.post(this)
    }

    override fun run() {
        Log.d(tag,"run()")
        userRepository.getCurrentUserSettings(object: Repository.Callback<Settings>{
            override fun onSuccess(data: Settings?) {
                Log.d(tag,"got user settings: "+data)
                if (data == null){
                    this@GetAppointmentStateUseCaseImpl.onError(com.pitstop.network.RequestError.getUnknownError())
                    return
                }

                val disposable = appointmentRepository.getAllAppointments(data.carId)
                        .subscribeOn(Schedulers.computation())
                        .observeOn(Schedulers.io())
                        .subscribe({response ->
                            Log.d(tag,"appointments: "+response)
                            //null if none found
                            val leastRecentAppointment = getLeastRecentAppointment(response)
                            Log.d(tag,"leastRecentAppointment: "+leastRecentAppointment)
                            if (leastRecentAppointment != null){
                                shopRepository.getAllShops(object: Repository.Callback<List<Dealership>>{
                                    override fun onSuccess(dealershipList: List<Dealership>) {
                                        val dealer = dealershipList.find { it.id == leastRecentAppointment.shopId }
                                        if (dealer != null){
                                            this@GetAppointmentStateUseCaseImpl.onAppointmentBookedState(leastRecentAppointment,dealer)
                                        }else{
                                            Log.d(tag,"Couldn't find dealership matching appointment")
                                            this@GetAppointmentStateUseCaseImpl.onError(RequestError.getUnknownError())
                                        }
                                    }

                                    override fun onError(error: RequestError?) {
                                        this@GetAppointmentStateUseCaseImpl.onError(error)
                                    }
                                })
                            }else{
                                //Get predicted service
                                val disposable = appointmentRepository.getPredictedService(data.carId)
                                        .subscribeOn(Schedulers.computation())
                                        .observeOn(Schedulers.io())
                                        .subscribe({response ->
                                            Log.d(tag,"Got predicted service: "+response)
                                            this@GetAppointmentStateUseCaseImpl.onPredictedServiceState(response)
                                        },{error ->
                                            Log.d(tag,"error getting predicted service" +
                                                    ", err.localizedMessage: ${error.localizedMessage}" +
                                                    ", error.message: ${error.message.toString()}")
                                            if (error.message != null && error.message!!.contains("HTTP 400")){
                                                Log.d(tag,"Contains not enough")
                                                this@GetAppointmentStateUseCaseImpl.onMileageUpdateNeededState()
                                            }else{
                                                this@GetAppointmentStateUseCaseImpl.onError(RequestError(error))
                                            }
                                        })
                                compositeDisposable.add(disposable)
                            }
                        }, {error ->
                            Log.d(tag,"error: "+error)
                            this@GetAppointmentStateUseCaseImpl.onError(RequestError(error))
                        })
                compositeDisposable.add(disposable)
            }

            override fun onError(error: RequestError?) {
                Log.d(tag,"error getting settings, err: "+error)
                this@GetAppointmentStateUseCaseImpl.onError(error!!)
            }

        })
    }

    //Returns the appointment scheduled most in the future
    //null if none found
    private fun getLeastRecentAppointment(appointments: List<Appointment>): Appointment?{
        var leastRecent: Date = Date() //Starts as today
        var leastRecentAppointment: Appointment? = null
        appointments
                .asSequence()
                .filter { it.date.after(leastRecent) }
                .forEach {
                    leastRecentAppointment = it
                    leastRecent = it.date
                }
        return leastRecentAppointment
    }

    private fun onMileageUpdateNeededState(){
        Logger.getInstance()!!.logI(tag, "Use case finished: onMileageUdateNeededState"
                , DebugMessage.TYPE_USE_CASE)
        compositeDisposable.clear()
        mainHandler.post({callback!!.onMileageUpdateNeededState()})
    }

    private fun onAppointmentBookedState(appointment: Appointment, dealership: Dealership){
        Logger.getInstance()!!.logI(tag, "Use case finished: onAppointmentBookedState: "
                +appointment+", dealership: "+dealership, DebugMessage.TYPE_USE_CASE)
        compositeDisposable.clear()
        mainHandler.post({callback!!.onAppointmentBookedState(appointment, dealership)})
    }

    private fun onPredictedServiceState(predictedService: PredictedService){
        Logger.getInstance()!!.logI(tag, "Use case finished: onPredictedServiceState: "
                +predictedService, DebugMessage.TYPE_USE_CASE)
        compositeDisposable.clear()
        mainHandler.post({callback!!.onPredictedServiceState(predictedService)})
    }

    private fun onError(requestError: RequestError?){
        val error: RequestError = if (requestError != null) requestError else RequestError.getUnknownError()
        Logger.getInstance()!!.logE(tag, "Use case returned error: err="
                + error, DebugMessage.TYPE_USE_CASE)
        compositeDisposable.clear()
        mainHandler.post({callback!!.onError(error)})
    }
}