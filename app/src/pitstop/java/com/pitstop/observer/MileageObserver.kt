package com.pitstop.observer

/**
 * Created by ishan on 2017-11-23.
 */
interface MileageObserver: Observer {

    fun onMileageAndRtcGot(mileage:String, rtc: String )
    fun onGetMileageAndRtcError();
    fun onNotConnected();

}