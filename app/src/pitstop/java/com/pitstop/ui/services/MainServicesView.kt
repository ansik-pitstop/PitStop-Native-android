package com.pitstop.ui.services

import com.pitstop.models.Appointment
import com.pitstop.retrofit.PredictedService

/**
 * Created by Karol Zdebel on 2/1/2018.
 */

interface MainServicesView {
    enum class ServiceTab(val tabNum: Int) {
        CURRENT(1), UPCOMING(0), HISTORY(2)
    }

    //Select respective tab
    fun selectTab(tab: ServiceTab)

    //Display layout #1
    fun displayMileageUpdateNeeded()
    //Called when button ?Update Mileage? is pressed
    fun onUpdateMileageClicked()
    //Displays dialog which allows the input of mileage
    fun displayMileageInputDialog()
    //Called after a mileage has been entered by the user
    fun onMileageInput()


    //Display booked appointment
    fun displayAppointmentBooked(appointment: Appointment)

    //Display predicted service
    fun displayPredictedService(predictedAppointment: PredictedService)
    //Invoked when button ?Request An Appointment? is clicked
    fun onRequestAppointmentClicked()
    //Go to RequestServiceActivity
    fun beginRequestService()

    //Displays error message in dialog
    fun displayErrorMessage(message: String)

}
