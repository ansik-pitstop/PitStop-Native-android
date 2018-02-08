package com.pitstop.ui.services

import android.os.Handler
import android.util.Log
import com.pitstop.EventBus.EventSource
import com.pitstop.EventBus.EventSourceImpl
import com.pitstop.EventBus.EventType
import com.pitstop.EventBus.EventTypeImpl
import com.pitstop.R
import com.pitstop.R.id.mileage
import com.pitstop.dependency.UseCaseComponent
import com.pitstop.interactors.get.GetAppointmentStateUseCase
import com.pitstop.interactors.update.UpdateCarMileageUseCase
import com.pitstop.models.Appointment
import com.pitstop.models.Dealership
import com.pitstop.network.RequestError
import com.pitstop.retrofit.PredictedService
import com.pitstop.ui.mainFragments.TabPresenter
import com.pitstop.utils.MixpanelHelper
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Created by Karol Zdebel on 2/1/2018.
 */
public class MainServicesPresenter(private val usecaseComponent: UseCaseComponent
                                   , private val mixpanelHelper: MixpanelHelper): TabPresenter<MainServicesView>() {

    private val tag = javaClass.simpleName
    val ignoredEvents = arrayOf<EventType>(EventTypeImpl(EventType.EVENT_SERVICES_HISTORY)
            , EventTypeImpl(EventType.EVENT_DTC_NEW), EventTypeImpl(EventType.EVENT_MILEAGE)
            , EventTypeImpl(EventType.EVENT_SCANNER), EventTypeImpl(EventType.EVENT_SERVICES_NEW))
    val EVENT_SOURCE: EventSource = EventSourceImpl(EventSource.SOURCE_MAIN_SERIVCES)

    //Find out which view (#1, #2, or #3) is appropriate to display and command the view to do so
    //Should be called when the view is created, typically after the subscribe() call is made
    fun loadView(){
        Log.d(tag,"loadView()")

        usecaseComponent.appointmentStateUseCase.execute(object: GetAppointmentStateUseCase.Callback{
            override fun onPredictedServiceState(predictedService: PredictedService) {
                Log.d(tag,"appointment state onPredictedServiceState() predictedService: "+predictedService);
                if (view != null){
                    val halfInterval: Long = predictedService.confidenceInterval/2L
                    val timeDiff = halfInterval * TimeUnit.DAYS.toMillis(halfInterval) //interval * milliseconds in day
                    val from = Date(predictedService.predictedDate.time-timeDiff)
                    val to = Date(predictedService.predictedDate.time+timeDiff)
                    Log.d(tag,"from: $from, to: $to")
                    val format = SimpleDateFormat("EEE MMM dd yyyy", Locale.CANADA)
                    view!!.displayPredictedService(format.format(from),format.format(to))
                }
            }

            override fun onAppointmentBookedState(appointment: Appointment, dealership: Dealership) {
                Log.d(tag,"appointment state onAppointmentBookedState() appointment: " +
                        "$appointment dealership: $dealership");
                val format = SimpleDateFormat("EEE MMM dd hh:mm a z yyyy",Locale.CANADA)
                if (view != null) view!!.displayAppointmentBooked(format.format(appointment.date)
                        , dealership.name)
            }

            override fun onMileageUpdateNeededState() {
                Log.d(tag,"appointment state onMileageUpdateNeededState()");
                if (view != null) view!!.displayMileageUpdateNeeded()
            }

            override fun onError(error: RequestError) {
                Log.d(tag,"appointment state onError() err"+error);
                if (view != null) view!!.displayErrorMessage(error.message)
            }
        })
    }

    //Should prompt view to open dialog
    fun onMileageUpdateClicked(){
        Log.d(tag,"onMileageUpdateClicked()")
        if (view != null) view!!.displayMileageInputDialog()
    }

    //Launch update mileage use case
    fun onMileageUpdateInput(input: String){
        Log.d(tag,"onMileageUpdateInput() input: "+mileage)
        if(view == null) return
        val mileage = input.toIntOrNull()
        if (mileage == null || mileage < 0 || mileage > 3000000){
            view!!.displayErrorMessage(R.string.invalid_mileage_alert_message)
        }else{
            usecaseComponent.updateCarMileageUseCase().execute(Integer.valueOf(mileage).toDouble(), object: UpdateCarMileageUseCase.Callback{
                override fun onMileageUpdated() {
                    Log.d(tag,"update mileage onMileageUpdated()")
                    if (view != null){
                        Handler().postDelayed({loadView()},5000)
                        view!!.displayWaitingForPredictedService()
                    }
                }

                override fun onNoCarAdded() {
                    Log.d(tag,"update mileage onNoCarAdded()")
                    if (view != null) view!!.displayErrorMessage("Please add a car")
                }

                override fun onError(error: RequestError?) {
                    Log.d(tag, "update mileage onError() err: "+error)
                    if (view != null) view!!.displayErrorMessage(error!!.message)
                }

            })
        }
    }

    fun onRefresh(){
        Log.d(tag,"onRefresh")
        loadView()
    }

    //Invoke beginRequestService() on view
    fun onRequestAppointmentClicked(){
        Log.d(tag,"onRequestAppointmentClicked()");
        if (view != null) view!!.beginRequestService()
    }

    fun onServiceRequested(){
        Log.d(tag,"onServiceRequested()")
        loadView()
    }

    override fun getIgnoredEventTypes(): Array<EventType> = ignoredEvents

    override fun onAppStateChanged() = loadView()

    override fun getSourceType() = EVENT_SOURCE

}