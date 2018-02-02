package com.pitstop.repositories

import com.pitstop.models.Appointment
import org.junit.Test

/**
 * Created by Karol Zdebel on 2/2/2018.
 */
class AppointmentRepositoryTest {

    @Test
    fun getAppointment() {
    }

    @Test
    fun requestAppointment() {
        //Input
        val appoinment = generateAppointment()
        val userId = 1559
        val carId = 5622

    }

    @Test
    fun getAllAppointments() {
    }

    fun generateAppointment(): Appointment{
        val state = "tentative"
        val date = "2016-12-01 14:00:00"
        val comments = "john"
        val shopId = 3
        return Appointment(shopId,state,date,comments)
    }



}