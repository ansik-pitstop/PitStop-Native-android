package com.pitstop.utils

import com.pitstop.models.Car
import com.pitstop.models.User
import io.smooch.core.Message
import io.smooch.core.Smooch
import java.util.*

/**
 * Created by Karol Zdebel on 6/20/2018.
 */
class SmoochUtil {
    companion object {
        fun setSmoochProperties(car: Car){
//            val user = io.smooch.core.User.getCurrentUser()
//            val customProperties = HashMap<String, Any>()
//            customProperties["VIN"] = car.vin
//            customProperties["Car Make"] = car.make
//            customProperties["Car Model"] = car.model
//            customProperties["Car Year"] = car.year
//            user.addMetadata(customProperties)
        }

        fun setSmoochProperties(user: User){
//            val smoochUser = io.smooch.core.User.getCurrentUser()
//            smoochUser.firstName = user.firstName ?: ""
//            smoochUser.lastName = user.lastName ?: ""
//            smoochUser.email = user.email
        }

        fun setSmoochProperties(user: User, car: Car) {
//            setSmoochProperties(car)
//            setSmoochProperties(user)
        }

        fun sendSignedUpSmoochMessage(firstName: String, lastName: String){
//            Smooch.getConversation()!!
//                    .sendMessage(Message("$firstName $lastName has signed up for Pitstop!"))
        }

        fun sendUserAddedCarSmoochMessage(user:User, car: Car){
//            Smooch.getConversation()!!
//                    .sendMessage(Message("${user.firstName ?: ""} ${user.lastName ?: ""} has added a ${car.make} ${car.model} ${car.year}"))
        }

        fun sendMessage(text: String) {
//            Smooch.getConversation()!!
//                    .sendMessage(Message(text))
        }
    }
}