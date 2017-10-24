package com.pitstop.ui.main_activity

import android.app.Fragment
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.pitstop.EventBus.EventSource
import com.pitstop.R
import com.pitstop.dependency.UseCaseComponent
import com.pitstop.interactors.get.GetCarByCarIdUseCase
import com.pitstop.interactors.get.GetCarsWithDealershipsUseCase
import com.pitstop.interactors.get.GetUserCarUseCase
import com.pitstop.interactors.set.SetUserCarUseCase
import com.pitstop.models.Car
import com.pitstop.models.Dealership
import com.pitstop.network.RequestError
import com.pitstop.ui.Presenter
import com.pitstop.ui.my_garage.MyGarageView
import com.pitstop.ui.service_request.RequestServiceActivity
import com.pitstop.utils.MixpanelHelper
import io.smooch.core.User
import java.util.ArrayList
import java.util.HashMap

/**
 * Created by ishan on 2017-10-20.
 */
class MainActivityPresenter(val useCaseCompnent: UseCaseComponent, val mixpanelHelper: MixpanelHelper) : Presenter<MainView> {

    var view : MainView? = null
    val TAG:String = this.javaClass.simpleName
    private var isLoading: Boolean = false
    private var mCar:Car? = null
    private var customProperties: HashMap<String, Any>? = null
    private var isCarLoaded: Boolean = false
    private var carListLoaded: Boolean = false
    private var dealershipListLoaded  = false
    var carList: MutableList<Car> = ArrayList()
    var dealershipList: MutableList<Dealership> = ArrayList()



    override fun subscribe(view: MainView?) {
        this.view = view
    }

    override fun unsubscribe() {
        Log.d(TAG, "unsubscribe()")
        this.view = null
    }

    fun onUpdateNeeded(){
        Log.d(TAG, "onUpdateNeeded")
        this.isCarLoaded = false
        mCar = null
        this.carListLoaded = false;
        this.dealershipListLoaded = false
        loadCars()
    }

    private fun loadCars() {
        Log.d(TAG, "loadCars()")
        if (!carListLoaded){
            view?.showCarsLoading()
            isLoading  = true
            useCaseCompnent.carsWithDealershipsUseCase.execute(object : GetCarsWithDealershipsUseCase.Callback{

                override fun onGotCarsWithDealerships(data: LinkedHashMap<Car, Dealership>) {
                    Log.d(TAG, "onCarsRetrieved")
                    isLoading = false
                    if(view == null)return
                    view?.hideCarsLoading()
                    if (data.keys.size == 0){
                        view?.noCarsView()
                    }
                    for(car in data.keys){
                        if (car.isCurrentCar)
                            mCar = car;
                        isCarLoaded = true;
                    }
                    mergeSetWithCarList(data.keys)
                    mergeSetWithDealershipList(data.values)
                    dealershipListLoaded = true
                    carListLoaded = true
                    view?.showCars(carList)

                }

                override fun onError(error: RequestError) {
                    isLoading = false
                    if (view == null) return
                    view?.hideCarsLoading()
                    // do nothing
                }
            })
        }
        else {
            if(view == null)return
            view?.hideCarsLoading()
            // do nothing
        }
    }

    fun onMyAppointmentsClicked() {
        Log.d(TAG, "onMyAppointmentsClicked()")
        if (this.view == null) return;
        view?.hideLoading()
        if (this.mCar?.dealership == null) {
            view?.toast("Please add a dealership to your car")
            return
        }
        view?.openAppointments(mCar!!);
    }

    fun onRequestServiceClicked() {
        Log.d(TAG, "onRequestServiceCLicked()")
        if (this.view == null) return;
        view?.hideLoading()
        if (this.mCar?.dealership == null) {
            view?.toast("Please add a dealership to your car")
            return
        }
        view?.openRequestService(mCar);


    }

    private fun mergeSetWithCarList(data: Set<Car>) {
        carList.clear()
        carList.addAll(data)
    }

    private fun mergeSetWithDealershipList(data: Collection<Dealership>) {
        dealershipList.clear()
        dealershipList.addAll(data)
    }

    fun onAddCarClicked() {
        view?.openAddCarActivity();
    }

    fun onMessageClicked() {
        Log.d(TAG, "onMessageClicked()")
        if (view == null || isLoading) return
        isLoading = true
        if (customProperties == null){
            if (view == null) return
            view?.hideLoading()
            customProperties = HashMap()
            if (mCar == null) {
                //TODO
                return
            }
            customProperties?.put("VIN", mCar!!.getVin())
            customProperties?.put("Car Make", mCar!!.getMake())
            customProperties?.put("Car Model", mCar!!.getModel())
            customProperties?.put("Car Year", mCar!!.getYear())
            Log.i(TAG, mCar?.getDealership()?.email)
            customProperties?.put("Email", mCar?.getDealership()!!.email)
            User.getCurrentUser().addProperties(customProperties)
            if (!(view?.isUserNull())!!) {
                customProperties?.put("Phone", view?.getUserPhone()?: "" )
                User.getCurrentUser().firstName = view?.getUserFirstName()
                User.getCurrentUser().email = view?.getUserEmail()
            }
            isLoading  = false
            view?.openSmooch()
        }
        else {
            isLoading = false
            if (view == null) return
            view?.hideLoading()
            view?.openSmooch()
        }
    }

    fun onCallClicked() {
        if (!isCarLoaded){
            view?.toast("still loading vehicle information")
            return
        }
        if (mCar?.dealership == null)return
        if (mCar?.dealership?.id?.equals(1)!!){
            view?.toast("Please add a dealership first")
            return
        }
        view?.callDealership(mCar?.dealership)
    }

    fun onFindDirectionsClicked() {
        if (!isCarLoaded){
            view?.toast("still loading vehicle information")
            return
        }
        if (mCar?.dealership == null)return
        if (mCar?.dealership?.id?.equals(1)!!){
            view?.toast("Please add a dealership first")
            return
        }
        view?.openDealershipDirections(mCar?.dealership)
    }

    fun makeCarCurrent(car: Car) {
        Log.d(TAG, "makeCarCurrent()")
        if (view == null || isLoading) return
        if (car.isCurrentCar)return
        isLoading = true
       useCaseCompnent.setUseCarUseCase().execute(car.id, EventSource.SOURCE_DRAWER, object : SetUserCarUseCase.Callback {
            override fun onUserCarSet() {
                for (currCar in carList){
                    if (currCar.id == car.id){
                        currCar.isCurrentCar = true
                        mCar = currCar
                    }
                    else
                        currCar.isCurrentCar = false
                }
                view?.notifyCarDataChanged()
                isLoading = false
                if (view == null) return
                view?.toast("Current car set")
            }

            override fun onError(error: RequestError) {
                isLoading = false
                if (view == null) return
                view?.toast(error.message)
            }
        })
    }


}