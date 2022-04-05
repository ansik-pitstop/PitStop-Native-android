package com.pitstop.ui.service_request_fleet_manager

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.pitstop.dependency.UseCaseComponent
import com.pitstop.interactors.get.GetCurrentServicesUseCase
import com.pitstop.interactors.get.GetCurrentUserUseCase
import com.pitstop.interactors.get.GetUserCarUseCase
import com.pitstop.interactors.other.SendFleetManagerSmsUseCase
import com.pitstop.models.Car
import com.pitstop.models.Dealership
import com.pitstop.models.User
import com.pitstop.models.issue.CarIssue
import com.pitstop.network.RequestError
import com.pitstop.repositories.Repository
import com.pitstop.utils.SmoochUtil
import io.reactivex.Single
import java.util.*
import kotlin.math.min

class ServiceRequestFleetManagerPresenter(val component: UseCaseComponent) {

    fun getUserInformation(): Single<User> {
        return Single.create {
            component.getCurrentUserUseCase.execute(object: GetCurrentUserUseCase.Callback {
                override fun onUserRetrieved(user: User?) {
                    it.onSuccess(user!!)
                }
                override fun onError(error: RequestError?) {}
            })
        }
    }

    fun getVehicleInformation(carId: Int?): Single<Car> {
        return Single.create {
            if (carId == null) return@create

            component.userCarUseCase.execute(carId, Repository.DATABASE_TYPE.LOCAL, object: GetUserCarUseCase.Callback {
                override fun onCarRetrieved(car: Car?, dealership: Dealership?, isLocal: Boolean) {
                    it.onSuccess(car!!)
                }

                override fun onNoCarSet(isLocal: Boolean) {}
                override fun onError(error: RequestError?) {}
            })
        }
    }

    fun getAddress(context: Context): Single<String> {
        return Single.create { single ->
            val fusedLocationProvider = LocationServices.getFusedLocationProviderClient(context)
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                single.onError(Error("Location not accepted"))
            }
            fusedLocationProvider.lastLocation.addOnSuccessListener {
                val geocoder = Geocoder(context, Locale.getDefault())
                if (it == null) {
                    single.onSuccess("Could not load address")
                    return@addOnSuccessListener
                }
                val addresses = geocoder.getFromLocation(it.latitude, it.longitude, 1)

                val address = addresses[0].getAddressLine(0) // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
                val city = addresses[0].locality
                val state = addresses[0].adminArea
                val country = addresses[0].countryName
                val postalCode = addresses[0].postalCode
                val knownName = addresses[0].featureName // Only if available else return NULL
                single.onSuccess(address)
            }
        }

    }

    fun getActiveDtcs(carId: Int?): Single<List<CarIssue>> {
        return Single.create {
            if (carId == null) {
                return@create
            }
            component.currentServicesUseCase.execute(carId, object: GetCurrentServicesUseCase.Callback {
                override fun onGotCurrentServices(currentServices: MutableList<CarIssue>?, customIssues: MutableList<CarIssue>?, local: Boolean) {
                    val services = mutableListOf<CarIssue>()
                    if (currentServices != null) services.addAll(currentServices)
                    if (customIssues != null) services.addAll(customIssues)
                    if (services.size != 0) {
                        it.onSuccess(services.sortedBy { it.priority * -1}.subList(0, min(3, services.size - 1)))
                    } else {
                        it.onSuccess(services)
                    }
                }

                override fun onNoCarAdded() {
                    print("no")
                }
                override fun onError(error: RequestError?) {
                    print(error)
                }
            })
        }
    }


    fun sendSmoochMessageWithTexts(texts: Array<String>, sendSms: Boolean) {
        var finalText = "- Service Request\n"
        finalText += texts.reduce { acc, s -> acc + "\n" + s }
        if (sendSms) {
            val smsText = finalText.slice(IntRange(0, min(150,finalText.length-1)))
            this.component.sendFleetManagerSmsUseCase().execute(smsText, object: SendFleetManagerSmsUseCase.Callback {
                override fun onSuccess() {

                }

                override fun onError(err: RequestError) {
                }
            })
        }


//        print(finalText)
        SmoochUtil.sendMessage(finalText)
    }

}