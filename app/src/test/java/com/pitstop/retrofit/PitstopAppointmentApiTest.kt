package com.pitstop.retrofit

import org.junit.Test

/**
 * Created by Karol Zdebel on 2/2/2018.
 */
class PitstopAppointmentApiTest {
    @Test
    fun appointmentsApi(){
        RetrofitTestUtil.getAppointmentApi().getAppointments(5622)
    }
}