package com.pitstop.utils

import com.pitstop.models.Car
import com.pitstop.models.User
import io.smooch.core.Smooch
import java.util.*

/**
 * Created by Karol Zdebel on 6/20/2018.
 */
class SmoochUtil {
    companion object {
        fun setSmoochProperties(car: Car){
            val user = io.smooch.core.User.getCurrentUser()
            val customProperties = HashMap<String, Any>()
            customProperties["Car Year"] = car.year
            if (car.vin != null) customProperties["VIN"] = car.vin
            if (car.make != null) customProperties["Car Make"] = car.make
            if (car.model != null) customProperties["Car Model"] = car.model
            user.addProperties(customProperties)
        }

        fun setSmoochProperties(user: User){
            val smoochUser = io.smooch.core.User.getCurrentUser()
            smoochUser.firstName = user.firstName ?: ""
            smoochUser.lastName = user.lastName ?: ""
            smoochUser.email = user.email
        }

        fun setSmoochProperties(user: User, car: Car){
            setSmoochProperties(car)
            setSmoochProperties(user)
        }

        fun sendSignedUpSmoochMessage(firstName: String, lastName: String){
            Smooch.getConversation()!!
                    .sendMessage(io.smooch.core.Message("$firstName $lastName has signed up for Pitstop!"))
        }

        fun sendUserAddedCarSmoochMessage(user:User, car: Car){
            Smooch.getConversation()!!
                    .sendMessage(io.smooch.core.Message("${user.firstName ?: ""} ${user.lastName ?: ""} has added a ${car.make} ${car.model} ${car.year}"))
        }

    }
};