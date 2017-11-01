package com.pitstop.interactors.get

import android.os.Handler
import android.util.Log
import com.pitstop.database.LocalAlarmStorage
import com.pitstop.interactors.add.AddAlarmUseCase
import com.pitstop.models.Alarm
import com.pitstop.network.RequestError
import com.pitstop.repositories.CarRepository
import com.pitstop.repositories.Repository
import com.pitstop.repositories.UserRepository
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * Created by ishan on 2017-10-30.
 */

class GetAlarmsUseCaseImpl(val localAlarmStorage: LocalAlarmStorage,
                           val useCaseHandler: Handler, val mainHandler: Handler): GetAlarmsUseCase {

    private val TAG = javaClass.simpleName;
    private var callback: GetAlarmsUseCase.Callback? = null
    private var carId: Int? = null



    override fun execute(carID: Int, callback: GetAlarmsUseCase.Callback) {
        this.carId = carID
        this.callback = callback
        useCaseHandler.post(this)
    }

    override fun run() {
        localAlarmStorage.getAlarms(carId!!, object : Repository.Callback<List<Alarm>>{
            override fun onSuccess(data: List<Alarm>?) {
                Log.d(TAG, "getAlarmsSuccess()");
                val arrayListAlarm: ArrayList<Alarm>  = ArrayList(data)
                var map: HashMap<String, ArrayList<Alarm>> = HashMap();
                for (alarm in arrayListAlarm) {
                    val date = Date()
                    date.time = java.lang.Long.parseLong(alarm.rtcTime) * 1000
                    val currDate: String = (date.month.toString() + " " + date.day.toString() + " " + date.year.toString())
                    if (map.containsKey(currDate)){
                        map[currDate]?.add(alarm)
                    }
                    else {
                        var currArrayList: ArrayList<Alarm> = ArrayList();
                        currArrayList.add(alarm);
                        map.put(currDate, currArrayList);
                    }
                }
                mainHandler.post({callback?.onAlarmsGot(map)})
            }
            override fun onError(error: RequestError?) {
                Log.d(TAG, "getAlarmsError()")
                mainHandler.post({callback?.onError(RequestError.getUnknownError())})
            }
        })
    }
}