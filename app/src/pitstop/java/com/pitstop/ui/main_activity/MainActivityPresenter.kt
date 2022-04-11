package com.pitstop.ui.main_activity

import android.content.Context
import android.util.Log
import com.pitstop.BuildConfig
import com.pitstop.EventBus.*
import com.pitstop.application.GlobalVariables
import com.pitstop.application.GlobalVariables.Companion.getUserId
import com.pitstop.dependency.UseCaseComponent
import com.pitstop.interactors.check.CheckFirstCarAddedUseCase
import com.pitstop.interactors.get.GetCarsByUserIdUseCase
import com.pitstop.interactors.set.SetFirstCarAddedUseCase
import com.pitstop.interactors.set.SetUserCarUseCase
import com.pitstop.models.Car
import com.pitstop.models.Dealership
import com.pitstop.network.RequestError
import com.pitstop.ui.Presenter
import com.pitstop.utils.MixpanelHelper
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*

/**
 * Created by ishan on 2017-10-20.
 */
class MainActivityPresenter(val useCaseCompnent: UseCaseComponent, val mixpanelHelper: MixpanelHelper, val context: Context) : Presenter<MainView> {

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

    private fun getUserId(): Int? {
        return getUserId(context)
    }

    fun getmCar(): Car?{
        return mCar;
    }

    fun getmDealership(): Dealership?{
        return this.mDealership;
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
        this.carListLoaded = false
        this.dealershipListLoaded = false
        loadCars()
    }

    fun onShowCaseClosed(){
        Log.d(TAG,"onShowCaseClosed()")
        if (view != null)
            view!!.openServiceRequest()
    }

    fun onCarAdded(withDealer: Boolean){
        Log.d(TAG,"onCarAdded()")
//        GlobalVariables.setMainCarId(context, carId)
        view?.closeDrawer()
        onRefresh()
//        useCaseCompnent.checkFirstCarAddedUseCase()!!
//                .execute(object: CheckFirstCarAddedUseCase.Callback{
//
//                    override fun onFirstCarAddedChecked(added: Boolean) {
//                        Log.d(TAG,"checkFirstCarAddedUseCase() result: $added")
//                        if (!added){
//                            if (view != null && withDealer)
//                                view!!.showTentativeAppointmentShowcase()
//
//                            useCaseCompnent.setFirstCarAddedUseCase()!!
//                                    .execute(true, object : SetFirstCarAddedUseCase.Callback {
//                                        override fun onFirstCarAddedSet() {
//                                            //Variable has been set
//                                        }
//                                        override fun onError(error: RequestError) {
//                                            //Networking error logic here
//                                        }
//                                    })
//                        }
//                    }
//                    override fun onError(error: RequestError?) {
//                        //error logic here
//                    }})
    }

    private fun loadCars() {
        Log.d(TAG, "loadCars()")
        if (isLoading) return
        val userId = getUserId() ?: return
        if (!carListLoaded){
            view?.showCarsLoading()
            isLoading  = true


            useCaseCompnent.carsByUserIdUseCase.execute(userId, object: GetCarsByUserIdUseCase.Callback {
                override fun onCarsRetrieved(cars: MutableList<Car>?) {
                    Log.d(TAG, "onCarsRetrieved()")

                    isLoading = false
                    view?.hideCarsLoading()

                    if (view == null) {
                        return
                    }

                    if (cars == null || cars.size == 0) {
                        Log.d(TAG, "No cars retrieved")
                        view?.noCarsView()
                        return
                    }

                    isCarLoaded = true

                    for (car in cars) {
                        if (car.isCurrentCar) {
                            Log.d(TAG, "Found current car")
                            mCar = car
                            mDealership = car.shop
                            view?.showNormalLAyout()
                        }
                    }

                    dealershipListLoaded = true
                    carListLoaded = true
                    mergeSetWithCarList(cars.toSet())
                    view?.showCars(cars)
                }

                override fun onError(error: RequestError?) {

                    isLoading = false
                    if (view == null) return
                    view?.hideCarsLoading()
                    if (carList.isEmpty()){
                        view?.errorLoadingCars()
                    }
                }
            })

//            useCaseCompnent.carsWithDealershipsUseCase.execute(object : GetCarsWithDealershipsUseCase.Callback{
//                override fun onGotCarsWithDealerships(data: LinkedHashMap<Car, Dealership>, local: Boolean) {
//                }
//                override fun onError(error: RequestError) {
//                }
//            })
        }
        else {
            if(view == null)return
            view?.hideCarsLoading()
            // do nothing
        }
    }

    fun onUserWasInactiveOnCreate(){
        Log.d(TAG,"onUserWasInactiveOnCreate()")
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
        Log.d(TAG, "onRequestServiceCLicked()")
        view?.openServiceRequest()
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
        Log.d(TAG, "onAddCarCLicked()");
        view?.openAddCarActivity()
    }

    fun onMessageClicked() {
        Log.d(TAG, "onMessageClicked()")
        if (view == null) return

        view?.openSmooch()
    }

    fun onServiceRequestClicked() {
        Log.d(TAG, "onServiceRequestClicked()")
        if (view == null) return
        view?.openServiceRequest()
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

    fun updateCurrentCarFromUserSettings(car: Car) {
        mCar = car
        mDealership = car.shop
    }

    fun makeCarCurrent(car: Car) {
        Log.d(TAG, "makeCarCurrent() car: "+car)
        if (view == null) return
        if (car.isCurrentCar)return

//        var prevCurrCar: Car? = null
//        var selectedCar: Car? = null
//        var prevDealership: Dealership? = null
        for (currCar in carList){
            when {
                currCar.isCurrentCar -> {
//                    prevCurrCar = currCar
//                    prevDealership = currCar.shop
                    currCar.isCurrentCar = false
                }
                currCar.id == car.id -> {
                    currCar.isCurrentCar = true
                    mCar = currCar
                    mDealership = currCar.shop
//                    selectedCar = currCar
                }
                else -> currCar.isCurrentCar = false
            }
        }
        view?.notifyCarDataChanged()
        view?.closeDrawer()

//       useCaseCompnent.setUserCarUseCase().execute(car.id, EventSource.SOURCE_DRAWER, object : SetUserCarUseCase.Callback {
//            override fun onUserCarSet() {
//                isLoading = false
//                if (view == null) return
//            }
//
//            override fun onError(error: RequestError) {
//                isLoading = false
//                if (view == null) return
//                view?.toast(error.message)
//
//                //Revert car selection if an error occurs
//                selectedCar?.isCurrentCar = false
//                prevCurrCar?.isCurrentCar = true
//                mDealership = prevDealership
//                view?.notifyCarDataChanged()
//            }
//        })
    }


    fun onRefresh() {
        onUpdateNeeded()
    }

    fun onAddDealershipClicked() {
        Log.d(TAG, "onAddDealershipClciked()");
        if (view == null) return
        view?.startSelectShopActivity(this.mCar);
    }


}