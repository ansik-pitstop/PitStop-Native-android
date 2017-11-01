package com.pitstop.ui.alarms

import android.util.Log
import com.pitstop.dependency.UseCaseComponent
import com.pitstop.interactors.get.GetAlarmsUseCase
import com.pitstop.interactors.get.GetUserCarUseCase
import com.pitstop.models.Alarm
import com.pitstop.models.Car
import com.pitstop.models.Dealership
import com.pitstop.network.RequestError
import com.pitstop.utils.MixpanelHelper
import kotlin.collections.ArrayList

/**
 * Created by ishan on 2017-10-30.
 */
class AlarmsPresenter(val useCaseComponent: UseCaseComponent, val mixpanelHelper: MixpanelHelper) {

    private val TAG = javaClass.simpleName
    val alarmList = ArrayList<Alarm>()
    var alarmsView: AlarmsView? = null
    var currCarGot: Boolean = false
    var carMercedes: Boolean = false;
    var carId: Int? = null
    var alarmsMap : HashMap<String, ArrayList<Alarm>> = HashMap();

    fun subscribe(view: AlarmsView) {
        this.alarmsView = view
    }

    fun unsubscribe() {
        this.alarmsView = null
    }

    fun onUpdateNeeded() {
        if (!currCarGot) {
            useCaseComponent.userCarUseCase.execute(object : GetUserCarUseCase.Callback {
                override fun onCarRetrieved(car: Car?, dealership: Dealership?) {
                    currCarGot = true
                    carId = car?.id
                    carMercedes = (car?.shopId == 4 || car?.shopId == 18)
                }

                override fun onNoCarSet() {
                    // should never happen since they can't open this activity without a car
                }

                override fun onError(error: RequestError?) {
                }
            })
        }

        if (carId!=null){
            useCaseComponent.alarmsUseCase.execute(carId!!, object: GetAlarmsUseCase.Callback{
                override fun onAlarmsGot(alarms: HashMap<String, ArrayList<Alarm>>) {
                    if (alarmsView == null) return;
                    if (alarms.isEmpty()) alarmsView?.noAlarmsView();
                    else {
                        alarmsMap = alarms
                        alarmsView?.populateAlarms(carMercedes);
                    }
                }

                override fun onError(error: RequestError) {
                    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                }
            })
        }


    }
}