package com.pitstop.ui.services

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.design.widget.TextInputEditText
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.pitstop.R
import com.pitstop.application.GlobalApplication
import com.pitstop.dependency.ContextModule
import com.pitstop.dependency.DaggerUseCaseComponent
import com.pitstop.ui.main_activity.MainActivity
import com.pitstop.ui.service_request.RequestServiceActivity
import com.pitstop.ui.services.current.CurrentServicesFragment
import com.pitstop.ui.services.history.HistoryServicesFragment
import com.pitstop.ui.services.upcoming.UpcomingServicesFragment
import com.pitstop.utils.AnimatedDialogBuilder
import com.pitstop.utils.MixpanelHelper
import kotlinx.android.synthetic.main.fragment_services.*
import kotlinx.android.synthetic.main.layout_services_appointment_booked.*
import kotlinx.android.synthetic.main.layout_services_predicted_service.*
import kotlinx.android.synthetic.main.layout_services_update_mileage.*
import kotlinx.android.synthetic.main.layout_services_waiting_predicted_service.*

class MainServicesFragment : Fragment(), MainServicesView, ServiceErrorDisplayer {
    private val TAG = MainServicesFragment::class.java.simpleName

    private var servicesPager: SubServiceViewPager? = null
    private var tabLayout: TabLayout? = null
    private var presenter: MainServicesPresenter? = null

    private lateinit var currentServicesFragment: CurrentServicesFragment
    private lateinit var upcomingServicesFragment: UpcomingServicesFragment
    private lateinit var historyServicesFragment: HistoryServicesFragment

    private lateinit var mileageUpdateDialog: AlertDialog
    private var hasBeenPopulated = false

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d(TAG,"onCreateView()")
        hasBeenPopulated = false
        val rootview = inflater!!.inflate(R.layout.fragment_services, null)
        servicesPager = activity.findViewById(R.id.services_viewpager)

        val dialogLayout = LayoutInflater.from(
                activity).inflate(R.layout.dialog_input_mileage, null)
        val textInputEditText = dialogLayout
                .findViewById<View>(R.id.mileage_input) as TextInputEditText
        mileageUpdateDialog = AnimatedDialogBuilder(activity)
                .setAnimation(AnimatedDialogBuilder.ANIMATION_GROW)
                .setTitle("Update Mileage")
                .setView(dialogLayout)
                .setPositiveButton("Confirm") { dialog, which ->
                    presenter!!.onMileageUpdateInput(
                            textInputEditText.text.toString())
                }
                .setNegativeButton("Cancel") { dialog, which -> dialog.cancel() }
                .create()

        if (presenter == null){
            val usecaseComponent = DaggerUseCaseComponent.builder()
                    .contextModule(ContextModule(context.applicationContext))
                    .build()
            presenter = MainServicesPresenter(usecaseComponent
                    , MixpanelHelper(context.applicationContext as GlobalApplication))

        }

        return rootview
    }

    //For reference by child fragments
    override fun displayServiceErrorDialog(message: String){
        Log.d(tag,"displayServiceErrorDialog() message: "+message)
        Toast.makeText(context,message,Toast.LENGTH_LONG).show()
    }

    //For reference by child fragments
    override fun displayServiceErrorDialog(code: Int){
        Log.d(tag,"displayServiceErrorDialog() code: "+code)
        Toast.makeText(context,getText(code),Toast.LENGTH_LONG).show()
    }


    fun onServiceRequested(){
        Log.d(tag,"onServiceRequested()")
        if (presenter != null) presenter!!.onServiceRequested()
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        presenter!!.subscribe(this)
        presenter!!.loadView()
        update_mileage_button.setOnClickListener({presenter!!.onMileageUpdateClicked()})
        request_appointment_button.setOnClickListener({presenter!!.onRequestAppointmentClicked()})
    }

    override fun onDestroyView() {
        Log.d(TAG,"onDestroyView()")
        super.onDestroyView()
        if (presenter != null)
            presenter!!.unsubscribe()
        hasBeenPopulated = false
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

        currentServicesFragment = CurrentServicesFragment()
        upcomingServicesFragment = UpcomingServicesFragment()
        historyServicesFragment = HistoryServicesFragment()

        //Pass reference to swipe refresh layout so other fragments can hide loading and check status
        currentServicesFragment.setParentSwipeRefreshLayout(swipe_refresh)
        currentServicesFragment.setErrorMessageDisplayer(this)
        historyServicesFragment.setParentSwipeRefreshLayout(swipe_refresh)
        historyServicesFragment.setErrorMessageDisplayer(this)
        upcomingServicesFragment.setParentSwipeRefreshLayout(swipe_refresh)
        upcomingServicesFragment.setErrorMessageDisplayer(this)

        //Refresh all tabs including the appointment status
        swipe_refresh.setOnRefreshListener {
            currentServicesFragment.onRefresh()
            historyServicesFragment.onRefresh()
            upcomingServicesFragment.onRefresh()
            presenter!!.onRefresh()
        }

        servicesPager!!.adapter = ServicesAdapter(childFragmentManager
                , upcomingServicesFragment, currentServicesFragment,historyServicesFragment)
        tabLayout!!.getTabAt(1)!!.select()

    }

    override fun selectTab(tab: MainServicesView.ServiceTab) {
        Log.d(TAG,"selectTab() tabNum: "+tab.tabNum)
        if (servicesPager == null) {
            return
        }
        tabLayout!!.getTabAt(tab.tabNum)!!.select()
    }

    override fun displayMileageUpdateNeeded() {
        Log.d(TAG,"displayMileageUpdateNeeded()")
        hasBeenPopulated = true
        appointment_info_holder.visibility = View.VISIBLE
        layout_waiting_predicted_service.visibility = View.GONE
        layout_appointment_booked.visibility = View.GONE
        layout_predicted_service.visibility = View.GONE
        layout_update_mileage.visibility = View.VISIBLE
    }

    override fun displayMileageInputDialog() {
        Log.d(TAG,"displaMileageInputDialog")
        mileageUpdateDialog.show()
    }

    override fun displayAppointmentBooked(d: String,who: String) {
        Log.d(TAG,"displayAppointmentBooked() date: "+d);
        hasBeenPopulated = true
        appointment_info_holder.visibility = View.VISIBLE
        layout_waiting_predicted_service.visibility = View.GONE
        layout_appointment_booked.visibility = View.VISIBLE
        layout_predicted_service.visibility = View.GONE
        layout_update_mileage.visibility = View.GONE
        date.text = d
        booked_message.text = String.format(getText(R.string.service_appointment_booked_message).toString(),who)
    }

    override fun displayPredictedService(from: String, to: String) {
        Log.d(TAG,"displayPredictedService() from: $from ,to: $to")
        hasBeenPopulated = true
        appointment_info_holder.visibility = View.VISIBLE
        layout_waiting_predicted_service.visibility = View.GONE
        layout_appointment_booked.visibility = View.GONE
        layout_predicted_service.visibility = View.VISIBLE
        layout_update_mileage.visibility = View.GONE
        dateRange.text = "$from - $to"
    }

    override fun beginRequestService() {
        Log.d(TAG,"beginRequestService()")
        val intent = Intent(context, RequestServiceActivity::class.java)
        intent.putExtra(RequestServiceActivity.activityResult.EXTRA_FIRST_BOOKING, false)
        startActivityForResult(intent, MainActivity.RC_REQUEST_SERVICE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d(TAG,"onActivityResult() requestCode: "+requestCode)
        if (presenter != null && requestCode == MainActivity.RC_REQUEST_SERVICE
                && resultCode == RequestServiceActivity.activityResult.RESULT_SUCCESS){
            presenter!!.onServiceRequested();
        }else{
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun displayErrorMessage(message: String) {
        Log.d(TAG,"displayErrorMessage() message: "+message)
        if (!hasBeenPopulated){
            displayNoState()
        }else{
            Toast.makeText(context,message,Toast.LENGTH_LONG).show()
        }
    }

    override fun displayErrorMessage(code: Int) {
        Log.d(TAG,"displayErrorMessage() code: $code, string: ${getText(code)}")
        if (!hasBeenPopulated){
            displayNoState()
        }else{
            Toast.makeText(context,getText(code).toString(),Toast.LENGTH_LONG).show()
        }
    }

    override fun displayWaitingForPredictedService() {
        Log.d(TAG,"displayWaitingForPredictedServices()")
        appointment_info_holder.visibility = View.VISIBLE
        layout_waiting_predicted_service.visibility = View.VISIBLE
        layout_appointment_booked.visibility = View.GONE
        layout_predicted_service.visibility = View.GONE
        layout_update_mileage.visibility = View.GONE
    }

    override fun displayNoState() {
        Log.d(TAG,"displayNoState()")
        appointment_info_holder.visibility = View.GONE
    }
}
