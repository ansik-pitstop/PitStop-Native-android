package com.pitstop.utils

import com.pitstop.models.Car
import com.pitstop.models.User
import java.util.*

/**
 * Created by Karol Zdebel on 6/20/2018.
 */
class SmoochUtil {
    companion object {
        fun setSmoochProperties(car: Car){
            val user = io.smooch.core.User.getCurrentUser()
            val customProperties = HashMap<String, Any>()
            customProperties["VIN"] = car.vin
            customProperties["Car Make"] = car.make
            customProperties["Car Model"] = car.model
            customProperties["Car Year"] = car.year
            user.addProperties(customProperties)
        }

        fun setSmoochProperties(user: User, car: Car){
            val smoochUser = io.smooch.core.User.getCurrentUser()
            smoochUser.firstName = user.firstName
            smoochUser.email = user.email
            val customProperties = HashMap<String,Any>()
            customProperties["VIN"] = car.vin
            customProperties["Car Make"] = car.make
            customProperties["Car Model"] = car.model
            customProperties["Car Year"] = car.year
            customProperties["Email"] = car.shop.email
            customProperties["Phone"] = user.phone
            smoochUser.addProperties(customProperties)
        }

    }
}