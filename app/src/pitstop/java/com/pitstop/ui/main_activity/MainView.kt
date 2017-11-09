package com.pitstop.ui.main_activity

import com.pitstop.models.Car
import com.pitstop.models.Dealership

/**
 * Created by ishan on 2017-10-20.
 */
interface MainView  {
    fun openAppointments(car: Car)
    fun openRequestService(tentative: Boolean)
    fun toast(message: String)
    fun hideLoading()
    fun onCarClicked(car: Car)
    fun noCarsView()
    fun showCars(carList: MutableList<Car>)
    fun openAddCarActivity()
    fun isUserNull(): Boolean
    fun getUserPhone(): Any?
    fun getUserFirstName(): String?
    fun getUserEmail(): String?
    fun openSmooch()
    fun callDealership(dealership: Dealership?)
    fun openDealershipDirections(dealership: Dealership?)
    fun showCarsLoading()
    fun hideCarsLoading()
    fun notifyCarDataChanged()
    fun errorLoadingCars()
    fun showTentativeAppointmentShowcase()
}