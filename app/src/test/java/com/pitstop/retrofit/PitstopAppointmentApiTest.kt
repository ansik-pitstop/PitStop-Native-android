package com.pitstop.retrofit

import com.google.gson.JsonObject
import com.pitstop.models.Appointment
import io.reactivex.Observable
import junit.framework.Assert.assertTrue
import org.junit.Test

/**
 * Created by Karol Zdebel on 2/2/2018.
 */
class PitstopAppointmentApiTest {

    @Test
    fun requestServiceTest(){
        val appointmentIn = generateAppointment()
        val userIn = 1559
        val carIdIn = 5622

        requestService(appointmentIn,userIn,carIdIn).doOnNext({ next ->
            assertTrue(next == appointmentIn)
        })
    }

    private fun generateAppointment(): Appointment {
        val state = "tentative"
        val date = "2016-12-01 14:00:00"
        val comments = "john"
        val shopId = 3
        return Appointment(shopId,state,date,comments)
    }

    private fun requestService(app: Appointment, userId: Int, carId: Int): Observable<Appointment> {
        val body = JsonObject()
        body.addProperty("userId",userId)
        body.addProperty("carId",carId)
        body.addProperty("shopId",app.shopId)
        val options = JsonObject()
        options.addProperty("state",app.state)
        options.addProperty("appointmentDate",app.date)
        body.add("options",options)
        return RetrofitTestUtil.getAppointmentApi().requestService(body)
    }
}