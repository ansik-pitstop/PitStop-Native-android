package com.pitstop.ui.service_request_fleet_manager

import android.annotation.SuppressLint
import android.content.Intent
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.pitstop.R
import com.pitstop.dependency.ContextModule
import com.pitstop.dependency.DaggerUseCaseComponent
import com.pitstop.models.issue.CarIssue
import com.pitstop.ui.main_activity.MainActivity
import com.pitstop.ui.notifications.NotificationsActivity
import com.pitstop.ui.service_request.RequestServiceActivity
import io.reactivex.disposables.Disposable
import android.location.Geocoder
import android.view.WindowManager
import com.google.android.material.textfield.TextInputEditText
import java.util.*


/**
 * Created by Matthew on 2017-07-11.
 */
class ServiceRequestFleetManager : AppCompatActivity() {
    private val TAG = RequestServiceActivity::class.java.simpleName

    private val presenter by lazy {
        val useCaseComponent = DaggerUseCaseComponent.builder()
                .contextModule(ContextModule(applicationContext))
                .build()
        ServiceRequestFleetManagerPresenter(useCaseComponent)
    }


    private var userInformationDisposable: Disposable? = null
    private var carInformationDisposable: Disposable? = null
    private var activeDtcsDisposable: Disposable? = null
    private var addressDisposable: Disposable? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        setContentView(R.layout.activity_request_service_fleet_manager)

        val diverNameTextView = findViewById<TextView>(R.id.service_request_driver_name)
        val vehicleTextView = findViewById<TextView>(R.id.service_request_vehicle_information)
        val activeDtcsTextView = findViewById<TextView>(R.id.service_request_active_dtcs)
        val createRequestButton = findViewById<LinearLayout>(R.id.create_a_request_button)
        val serviceRequestLocation = findViewById<TextView>(R.id.service_request_location)
        val alertFleetManagerCheckBox = findViewById<CheckBox>(R.id.alert_fleet_manager_checkbox)
        val additionalInformation = findViewById<TextInputEditText>(R.id.service_request_additional_information)

        val userInformation = presenter.getUserInformation()
        userInformationDisposable = userInformation.subscribe { user, _ ->
            val name = "${user?.firstName ?: ""} ${user?.lastName ?: ""}"
            diverNameTextView.text = "Name: $name"
        }

        val vehicleInformation = presenter.getVehicleInformation()
        carInformationDisposable = vehicleInformation.subscribe { car, _ ->
            Log.d(TAG, car.toString())
            val vehicleText = "Vehicle: ${car.make} ${car.model} ${car.year} ${car.vin}"
            vehicleTextView.text = vehicleText
        }

        val activeDtcs = presenter.getActiveDtcs()
        activeDtcsDisposable = activeDtcs.subscribe { activeDtcs, _ ->
            activeDtcsTextView.text = "Priority: ${activeDtcs.fold("", { acc, carIssue -> carIssue.name + ", " + acc })}"
            activeDtcsTextView.text.drop(2)
            if (activeDtcs.isEmpty()) {
                activeDtcsTextView.text = "Priority: No issues detected"
            }
        }

        addressDisposable = presenter.getAddress(this)
            .subscribe { address ->
                serviceRequestLocation.text = "Location: ${address}"
            }

        createRequestButton.setOnTouchListener { view, motionEvent ->
            view.performClick()
            when (motionEvent.action) {
                MotionEvent.ACTION_UP -> {
                    var strings = mutableListOf(
                            diverNameTextView.text.toString(),
                            vehicleTextView.text.toString(),
                            activeDtcsTextView.text.toString(),
                            serviceRequestLocation.text.toString())

                    val additionalInformationText = additionalInformation.text.toString()
                    if (additionalInformationText.isNotEmpty()) {
                        strings.add("Additional information: $additionalInformationText")
                    }

                    presenter.sendSmoochMessageWithTexts(strings.toTypedArray(), alertFleetManagerCheckBox.isChecked)
                    setResult(NotificationsActivity.GO_TO_SMOOCH_MESSAGES)
                    finish()
                }
                else -> view.onTouchEvent(motionEvent)
            }
            true
        }

        Log.d(TAG, userInformation.toString())

    }

    override fun onDestroy() {
        super.onDestroy()
        userInformationDisposable?.dispose()
        carInformationDisposable?.dispose()
    }

    fun fillScreen(driverName: String, vehicleInformation: String, locationName: String, mainDtcs: String) {

    }
}