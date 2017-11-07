package com.pitstop.interactors.get

import android.os.Handler
import android.util.Log
import com.pitstop.database.LocalAlarmStorage
import com.pitstop.dependency.UseCaseComponent
import com.pitstop.interactors.add.AddAlarmUseCase
import com.pitstop.models.Alarm
import com.pitstop.models.Settings
import com.pitstop.network.RequestError
import com.pitstop.repositories.CarRepository
import com.pitstop.repositories.Repository
import com.pitstop.repositories.UserRepository
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.time.Month
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

    override fun execute( callback: GetAlarmsUseCase.Callback) {
        this.callback = callback
        useCaseHandler.post(this)
    }

    override fun run() {
        userRepository.getCurrentUserSettings( object: Repository.Callback<Settings>{
            override fun onSuccess(settings: Settings?) {
                    localAlarmStorage.getAlarms(settings?.carId!!, object : Repository.Callback<List<Alarm>>{
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
                                    Log.d(TAG, settings.toString())

                                    mainHandler.post({callback?.onAlarmsGot(map, settings.isAlarmsEnabled)})
                                }
                                override fun onError(error: RequestError?) {
                                    Log.d(TAG, "onGetShopIderror")
                                    mainHandler.post({callback?.onError(RequestError.getUnknownError())})
                                }

                    })
            }
            override fun onError(error: RequestError?) {
                Log.d(TAG, "getCurrentUSerError()")
                mainHandler.post({callback?.onError(RequestError.getUnknownError())})
            }
        })
    }
}