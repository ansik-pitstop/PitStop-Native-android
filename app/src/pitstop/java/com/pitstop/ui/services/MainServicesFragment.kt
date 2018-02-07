package com.pitstop.ui.services

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.pitstop.R
import com.pitstop.application.GlobalApplication
import com.pitstop.dependency.ContextModule
import com.pitstop.dependency.DaggerUseCaseComponent
import com.pitstop.ui.main_activity.MainActivity
import com.pitstop.ui.service_request.RequestServiceActivity
import com.pitstop.utils.MixpanelHelper
import kotlinx.android.synthetic.main.layout_services_appointment_booked.*
import kotlinx.android.synthetic.main.layout_services_predicted_service.*
import kotlinx.android.synthetic.main.layout_services_update_mileage.*
import kotlinx.android.synthetic.pitstop.fragment_services.*

class MainServicesFragment : Fragment(), MainServicesView {
    private val TAG = MainServicesFragment::class.java.simpleName

    private var servicesPager: SubServiceViewPager? = null
    private var tabLayout: TabLayout? = null
    private var presenter: MainServicesPresenter? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d(TAG,"onCreateView()")
        val rootview = inflater!!.inflate(R.layout.fragment_services, null)
        servicesPager = activity.findViewById(R.id.services_viewpager)

        if (presenter == null){
            val usecaseComponent = DaggerUseCaseComponent.builder()
                    .contextModule(ContextModule(context.applicationContext))
                    .build()
            presenter = MainServicesPresenter(usecaseComponent
                    , MixpanelHelper(context.applicationContext as GlobalApplication))

        }

        return rootview
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        presenter!!.subscribe(this)
        update_mileage_button.setOnClickListener({presenter!!.onMileageUpdateClicked()})
        request_appointment_button.setOnClickListener({presenter!!.onRequestAppointmentClicked()})

    }

    override fun onDestroyView() {
        Log.d(TAG,"onDestroyView()")
        super.onDestroyView()
        if (presenter != null)
            presenter!!.unsubscribe()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        Log.d(TAG,"onActivityCreated()")
        super.onActivityCreated(savedInstanceState)
        servicesPager = activity.findViewById<View>(R.id.services_viewpager) as SubServiceViewPager
        servicesPager!!.offscreenPageLimit = 2

        //Create tab layout
        tabLayout = activity.findViewById<View>(R.id.tab_layout) as TabLayout
        tabLayout!!.addTab(tabLayout!!.newTab().setText(getString(R.string.upcoming_services)))
        tabLayout!!.addTab(tabLayout!!.newTab().setText(getString(R.string.current_services)))
        tabLayout!!.addTab(tabLayout!!.newTab().setText(getString(R.string.history_services)))
        tabLayout!!.tabGravity = TabLayout.GRAVITY_FILL
        tabLayout!!.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                servicesPager!!.currentItem = tab.position
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {

            }

            override fun onTabReselected(tab: TabLayout.Tab) {

            }
        })

        servicesPager!!.adapter = ServicesAdapter(childFragmentManager)
        tabLayout!!.getTabAt(1)!!.select()

    }

    override fun selectTab(tab: MainServicesView.ServiceTab) {
        Log.d(TAG,"selectTab() tabNum: "+tab.tabNum);
        if (servicesPager == null) {
            return
        }
        tabLayout!!.getTabAt(tab.tabNum)!!.select()
    }

    override fun displayMileageUpdateNeeded() {
        Log.d(TAG,"displayMileageUpdateNeeded()")
        layout_appointment_booked.visibility = View.GONE
        layout_predicted_service.visibility = View.GONE
        layout_update_mileage.visibility = View.VISIBLE

    }

    override fun displayMileageInputDialog() {
        Log.d(TAG,"displaMileageInputDialog")
    }

    override fun onMileageInput() {
        Log.d(TAG,"onMileageInput()")
    }

    override fun displayAppointmentBooked(d: String) {
        Log.d(TAG,"displayAppointmentBooked() date: "+d);
        layout_appointment_booked.visibility = View.VISIBLE
        layout_predicted_service.visibility = View.GONE
        layout_update_mileage.visibility = View.GONE
        date.text = d
    }

    override fun displayPredictedService(from: String, to: String) {
        Log.d(TAG,"displayPredictedService() from: $from ,to: $to");
        layout_appointment_booked.visibility = View.GONE
        layout_predicted_service.visibility = View.VISIBLE
        layout_update_mileage.visibility = View.GONE
        dateRange.text = "$from - $to"
    }

    override fun beginRequestService() {
        Log.d(TAG,"beginRequestService()")
        val intent = Intent(context, RequestServiceActivity::class.java)
        intent.putExtra(RequestServiceActivity.EXTRA_FIRST_BOOKING, false)
        startActivityForResult(intent, MainActivity.RC_REQUEST_SERVICE)

    }

    override fun displayErrorMessage(message: String) {
        Log.d(TAG,"displayErrorMessage()")
    }

    override fun displayWaitingForPredictedService() {
        Log.d(TAG,"displayWaitingForPredictedServices()")
    }
}
