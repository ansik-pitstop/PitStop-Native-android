package com.pitstop.interactors.get

import android.os.Handler
import android.util.Log
import com.pitstop.database.LocalAlarmStorage
import com.pitstop.models.Alarm
import com.pitstop.models.DebugMessage
import com.pitstop.models.Settings
import com.pitstop.network.RequestError
import com.pitstop.repositories.CarRepository
import com.pitstop.repositories.Repository
import com.pitstop.repositories.UserRepository
import com.pitstop.utils.Logger
import java.text.DateFormatSymbols
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * Created by ishan on 2017-10-30.
 */

class GetAlarmsUseCaseImpl( val carRepository: CarRepository, val userRepository: UserRepository, val localAlarmStorage: LocalAlarmStorage,
                           val useCaseHandler: Handler, val mainHandler: Handler): GetAlarmsUseCase {

    private val TAG = javaClass.simpleName;
    private var callback: GetAlarmsUseCase.Callback? = null
    private var carId: Int = 0

    override fun execute(carId: Int, callback: GetAlarmsUseCase.Callback) {
        Logger.getInstance().logI(TAG, "Use case execution started"
                , DebugMessage.TYPE_USE_CASE)
        this.callback = callback
        this.carId = carId
        useCaseHandler.post(this)
    }

    override fun run() {
        localAlarmStorage.getAlarms(this.carId, object : Repository.Callback<List<Alarm>>{
            override fun onSuccess(list: List<Alarm>?) {
                Log.d(TAG, "getAlarmsSuccess()");
                val arrayListAlarm: ArrayList<Alarm>  = ArrayList(list)
                var map: HashMap<String, ArrayList<Alarm>> = HashMap();
                for (alarm in arrayListAlarm) {
                    val date = Date()
                    date.time = java.lang.Long.parseLong(alarm.rtcTime) * 1000
                    val currDate: String = (DateFormatSymbols().months[date.month]+ " " + date.date + " " + (1900 + date.year))
                    if (map.containsKey(currDate)){
                        map[currDate]?.add(alarm)
                    }
                    else {
                        var currArrayList: ArrayList<Alarm> = ArrayList();
                        currArrayList.add(alarm);
                        map.put(currDate, currArrayList);
                    }
                }

                Logger.getInstance().logI(TAG, "Use case finished: alarms:$map, enabled=true"
                        , DebugMessage.TYPE_USE_CASE)
                mainHandler.post({callback?.onAlarmsGot(map, true)})
            }
            override fun onError(error: RequestError?) {
                Logger.getInstance().logE(TAG, "Use case returned error: err="+error
                        , DebugMessage.TYPE_USE_CASE)
                mainHandler.post({callback?.onError(RequestError.getUnknownError())})
            }

        })
    }
}