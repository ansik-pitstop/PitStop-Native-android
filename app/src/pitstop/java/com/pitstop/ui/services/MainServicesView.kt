package com.pitstop.ui.services

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
    //Displays dialog which allows the input of mileage
    fun displayMileageInputDialog()
    //Called after a mileage has been entered by the user
    fun onMileageInput()
    //Called when predicted service date is loading on backend
    fun displayWaitingForPredictedService()


    //Display booked appointment
    fun displayAppointmentBooked(date: String)

    //Display predicted service
    fun displayPredictedService(from: String, to: String)
    //Go to RequestServiceActivity
    fun beginRequestService()

    //Displays error message in dialog
    fun displayErrorMessage(message: String)

}
