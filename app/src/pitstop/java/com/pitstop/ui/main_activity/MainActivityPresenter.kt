package com.pitstop.ui.main_activity

import android.util.Log
import com.pitstop.BuildConfig
import com.pitstop.EventBus.*
import com.pitstop.R.array.car
import com.pitstop.dependency.UseCaseComponent
import com.pitstop.interactors.check.CheckFirstCarAddedUseCase
import com.pitstop.interactors.get.GetCarsWithDealershipsUseCase
import com.pitstop.interactors.get.GetCurrentUserUseCase
import com.pitstop.interactors.get.GetUserCarUseCase
import com.pitstop.interactors.set.SetFirstCarAddedUseCase
import com.pitstop.interactors.set.SetUserCarUseCase
import com.pitstop.models.Car
import com.pitstop.models.Dealership
import com.pitstop.network.RequestError
import com.pitstop.ui.Presenter
import com.pitstop.utils.MixpanelHelper
import io.smooch.core.Smooch
import io.smooch.core.User
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*

/**
 * Created by ishan on 2017-10-20.
 */
class MainActivityPresenter(val useCaseCompnent: UseCaseComponent, val mixpanelHelper: MixpanelHelper) : Presenter<MainView> {

    var view : MainView? = null
    val TAG:String = this.javaClass.simpleName
    private var isLoading: Boolean = false
    private var mCar:Car? = null
    private var mDealership: Dealership? = null
    private var customProperties: HashMap<String, Any>? = null
    private var isCarLoaded: Boolean = false
    private var carListLoaded: Boolean = false
    private var dealershipListLoaded  = false
    var carList: MutableList<Car> = ArrayList()
    var dealershipList: MutableList<Dealership> = ArrayList()
    val EVENT_SOURCE: EventSource = EventSourceImpl(EventSource.SOURCE_DRAWER)
    val ignoredEvents = mutableListOf<EventType>(EventTypeImpl(EventType.EVENT_SERVICES_HISTORY),
                    EventTypeImpl(EventType.EVENT_DTC_NEW), EventTypeImpl(EventType.EVENT_MILEAGE),
                    EventTypeImpl(EventType.EVENT_SERVICES_NEW))

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onCarDataChangedEvent(event: CarDataChangedEvent) {

        /*Respond to event only if its EventType isn't being ignored
        * AND if it wasn't sent by this fragment*/
        if (!ignoredEvents.contains(event.eventType) && event.eventSource != EVENT_SOURCE) {
            onUpdateNeeded()
        }
    }


    fun getmCar(): Car?{
        return mCar;
    }
    fun getSourceType(): EventSource {
        return EVENT_SOURCE
    }



    override fun subscribe(view: MainView?) {
        this.view = view
        setNoUpdateOnEventTypes(ignoredEvents)
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }

    }

    private fun setNoUpdateOnEventTypes(eventTypes: MutableList<EventType>) {
        for (e in eventTypes) {
            if (!ignoredEvents.contains(e)) {
                ignoredEvents.add(e)
            }
        }
    }

    private fun hasDealership(): Boolean = (((BuildConfig.DEBUG || BuildConfig.BUILD_TYPE == BuildConfig.BUILD_TYPE_BETA)
                && mDealership != null && mDealership?.id != 1) || (BuildConfig.BUILD_TYPE == BuildConfig.BUILD_TYPE_RELEASE
                && mDealership != null && mDealership?.id != 19))

    override fun unsubscribe() {
        Log.d(TAG, "unSubscribe()")
        this.view = null
        EventBus.getDefault().unregister(this)
    }

    fun onUpdateNeeded(){
        Log.d(TAG, "onUpdateNeeded")
        this.isCarLoaded = false
        mCar = null
        this.carListLoaded = false;
        this.dealershipListLoaded = false
        loadCars()
    }

    fun onShowCaseClosed(){
        Log.d(TAG,"onShowCaseClosed()")
        if (view != null)
            view!!.openRequestService(true)
    }

    fun onCarAdded(withDealer: Boolean){
        Log.d(TAG,"onCarAdded()")
        updateSmoochUser()
        view?.closeDrawer()
        useCaseCompnent.checkFirstCarAddedUseCase()!!
                .execute(object: CheckFirstCarAddedUseCase.Callback{

                    override fun onFirstCarAddedChecked(added: Boolean) {
                        Log.d(TAG,"checkFirstCarAddedUseCase() result: $added")
                        if (!added){
                            sendSignedUpSmoochMessage()
                            if (view != null && withDealer)
                                view!!.showTentativeAppointmentShowcase()

                            useCaseCompnent.setFirstCarAddedUseCase()!!
                                    .execute(true, object : SetFirstCarAddedUseCase.Callback {
                                        override fun onFirstCarAddedSet() {
                                            //Variable has been set
                                        }
                                        override fun onError(error: RequestError) {
                                            //Networking error logic here
                                        }
                                    })
                        }
                    }
                    override fun onError(error: RequestError?) {
                        //error logic here
                    }})
    }

    private fun updateSmoochUser() {
        Log.d(TAG,"updateSmoochUser()");
        useCaseCompnent.getCurrentUserUseCase.execute(object: GetCurrentUserUseCase.Callback{
            override fun onUserRetrieved(user: com.pitstop.models.User) {
                Log.d(TAG,"onUserRetrieved() user: "+user)

                val customProperties: HashMap<String, Any?> = HashMap()
                customProperties.put("Phone", user.phone)
                User.getCurrentUser().firstName = user.firstName
                User.getCurrentUser().email = user.email
                User.getCurrentUser().addProperties(customProperties)

                useCaseCompnent.userCarUseCase.execute(object: GetUserCarUseCase.Callback{
                    override fun onCarRetrieved(car: Car, dealership: Dealership, isLocal: Boolean) {
                        if (isLocal) return
                        Log.d(TAG,"onCarRetrieved() car: "+car)

                        customProperties.put("VIN", car.vin)
                        Log.d(TAG, car.vin)
                        customProperties.put("Car Make", car.make)
                        Log.d(TAG, car.make)
                        customProperties.put("Car Model", car.model)
                        Log.d(TAG, car.model)
                        customProperties.put("Car Year", car.year)
                        Log.d(TAG, car.year.toString())
                        customProperties.put("Email", dealership.email)
                        Log.d(TAG, dealership.email)
                    }

                    override fun onNoCarSet(isLocal: Boolean) {
                        Log.d(TAG,"onNoCarSet() car: "+car)
                    }

                    override fun onError(error: RequestError) {
                        Log.d(TAG,"onError() err: "+error)
                    }

                })
            }

            override fun onError(error: RequestError) {
                Log.d(TAG,"onError() err: "+error)
            }

        })
    }

    private fun sendSignedUpSmoochMessage() {
        Log.d(TAG,"sendSignedUpSmoochMessage() ");
        useCaseCompnent.getCurrentUserUseCase.execute(object: GetCurrentUserUseCase.Callback{
            override fun onUserRetrieved(user: com.pitstop.models.User) {
                Log.d(TAG,"retrieved current user: "+user)
                Smooch.getConversation().sendMessage(io.smooch.core.Message(user.firstName +
                        (if (user.lastName == null || user.lastName == "null")
                            ""
                        else
                            " " + user.lastName) + " has signed up for Pitstop!"))
            }

            override fun onError(error: RequestError?) {
            }

        })
    }

    private fun loadCars() {
        Log.d(TAG, "loadCars()")
        if (isLoading) return
        if (!carListLoaded){
            view?.showCarsLoading()
            isLoading  = true
            useCaseCompnent.carsWithDealershipsUseCase.execute(object : GetCarsWithDealershipsUseCase.Callback{

                override fun onGotCarsWithDealerships(data: LinkedHashMap<Car, Dealership>, local: Boolean) {
                    Log.d(TAG, "onCarsRetrieved() local? $local, map: $data")
                    if (!local) isLoading = false
                    if(view == null)return
                    if (!local){
                        view?.hideCarsLoading()
                    }
                    if (data.keys.size == 0){
                        view?.noCarsView()
                    }
                    for(car in data.keys){
                        if (car.isCurrentCar) {
                            mCar = car
                            mDealership = data[mCar!!]
                            if (car.shopId == 4 || car.shopId == 18)
                                view?.showMercedesLayout()
                            else
                                view?.showNormalLAyout();
                        }
                        isCarLoaded = true
                    }
                    mergeSetWithCarList(data.keys)
                    mergeSetWithDealershipList(data.values)
                    if (!local){
                        dealershipListLoaded = true
                        carListLoaded = true
                    }
                    view?.showCars(carList)

                }

                override fun onError(error: RequestError) {
                    isLoading = false
                    if (view == null) return
                    view?.hideCarsLoading()
                    if (carList.isEmpty()){
                        view?.errorLoadingCars()
                    }
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
        if (this.view == null) return

        if (mCar == null){
            view?.toast("Please add a car first")
        }
        else if (!hasDealership()) {
            view?.toast("Please add a dealership to your car")
        }
        else{
            view?.openAppointments(mCar!!)
        }
    }

    fun onRequestServiceClicked() {
        Log.d(TAG, "onRequestServiceCLicked() mDealership: $mDealership")
        if (this.view == null) return
        if (mCar == null){
            view?.toast("Please add a car first")
        }else if (!hasDealership()) {
            view?.toast("Please add a dealership to your car")
        }
        else{
            view?.openRequestService(false)
        }


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
        view?.openAddCarActivity()
    }

    fun onMessageClicked() {
        Log.d(TAG, "onMessageClicked()")
        if (view == null) return
        if (customProperties == null){
            customProperties = HashMap()
            if (mCar != null) {
                customProperties?.put("VIN", mCar!!.getVin())
                customProperties?.put("Car Make", mCar!!.getMake())
                customProperties?.put("Car Model", mCar!!.getModel())
                customProperties?.put("Car Year", mCar!!.getYear())
                if (mDealership != null)
                    customProperties?.put("Email", mDealership!!.email)
            }
            if (!(view?.isUserNull())!!) {
                customProperties?.put("Phone", view?.getUserPhone()?: "" )
                User.getCurrentUser().firstName = view?.getUserFirstName()
                User.getCurrentUser().email = view?.getUserEmail()
            }
            User.getCurrentUser().addProperties(customProperties)
            view?.openSmooch()
        }
        else {
            view?.openSmooch()
        }
    }

    fun onCallClicked() {
        if (!isCarLoaded){
            view?.toast("Car data has not been loaded yet. Check your connection.")
            return
        }
        if (mCar == null){
            view?.toast("Please add a car first")
        }else if (!hasDealership()){
            view?.toast("Please add a dealership first")
        }else{
            view?.callDealership(mDealership)
        }
    }

    fun onFindDirectionsClicked() {
        if (!isCarLoaded){
            view?.toast("Car data has not been loaded yet. Check your connection.")
            return
        }
        if (mCar == null){
            view?.toast("Please add a car first")
        } else if (!hasDealership()){
            view?.toast("Please add a dealership first")
        }else{
            view?.openDealershipDirections(mDealership)
        }
    }

    fun makeCarCurrent(car: Car) {
        Log.d(TAG, "makeCarCurrent() car: "+car)
        if (view == null || isLoading) return
        if (car.isCurrentCar)return
        isLoading = true

        var prevCurrCar: Car? = null
        var selectedCar: Car? = null
        var prevDealership: Dealership? = null
        for (currCar in carList){
            when {
                currCar.isCurrentCar -> {
                    prevCurrCar = currCar
                    prevDealership = currCar.shop
                    currCar.isCurrentCar = false
                }
                currCar.id == car.id -> {
                    currCar.isCurrentCar = true
                    mCar = currCar
                    mDealership = currCar.shop
                    selectedCar = currCar
                }
                else -> currCar.isCurrentCar = false
            }
        }
        view?.notifyCarDataChanged()
        view?.closeDrawer()

       useCaseCompnent.setUserCarUseCase().execute(car.id, EventSource.SOURCE_DRAWER, object : SetUserCarUseCase.Callback {
            override fun onUserCarSet() {
                isLoading = false
                if (view == null) return
            }

            override fun onError(error: RequestError) {
                isLoading = false
                if (view == null) return
                view?.toast(error.message)

                //Revert car selection if an error occurs
                selectedCar?.isCurrentCar = false
                prevCurrCar?.isCurrentCar = true
                mDealership = prevDealership
                view?.notifyCarDataChanged()
            }
        })
    }

    fun onRefresh() {
        onUpdateNeeded()
    }


}