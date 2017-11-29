package com.pitstop.ui.alarms

import android.app.Fragment
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SwitchCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.TextView
import android.widget.Toast
import com.pitstop.R
import com.pitstop.adapters.AlarmsAdapter
import com.pitstop.application.GlobalApplication
import com.pitstop.dependency.ContextModule
import com.pitstop.dependency.DaggerUseCaseComponent
import com.pitstop.models.Alarm
import com.pitstop.observer.AlarmObservable
import com.pitstop.observer.AlarmObserver
import com.pitstop.utils.MixpanelHelper

/**
 * Created by ishan on 2017-10-30.
 */
class AlarmsFragment : AlarmsView, Fragment(), AlarmObserver{
    val TAG: String = javaClass.simpleName

    var presenter : AlarmsPresenter? = null
    var recyclerView :RecyclerView? = null
    var noALarmsView : View? = null
    var enableAlarmsCaption: TextView? = null
    var alarmsAdapter:AlarmsAdapter?  =null
    var errorLoadingAlarmsView: View? = null
    var loadingView : View? = null
    var alarmsObservable :AlarmObservable? = null
    var alarmsEnabledSwitch: SwitchCompat? = null
    var isDealershipMercedes: Boolean = false

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {
        Log.d(TAG, "onCreateView()" )
        val view : View?  = inflater?.inflate(R.layout.fragment_alarms, null)
        if ((activity as AlarmsActivity).autoConnectService != null){
            alarmsObservable = ((activity as AlarmsActivity).autoConnectService as AlarmObservable)
            alarmsObservable?.subscribe(this)
        }

        if (presenter == null) {
            val useCaseComponent = DaggerUseCaseComponent.builder()
                    .contextModule(ContextModule(activity))
                    .build()
            val mixpanelHelper = MixpanelHelper(activity
                    .applicationContext as GlobalApplication)
            presenter = AlarmsPresenter(useCaseComponent, mixpanelHelper)

        }
        isDealershipMercedes = arguments.getBoolean("isMercedes");
        presenter?.subscribe(this)
        noALarmsView = view?.findViewById(R.id.no_alarms_view)
        recyclerView = view?.findViewById(R.id.main_recycler_view)
        alarmsAdapter = AlarmsAdapter(presenter?.alarmsMap, activity, this)
        recyclerView?.layoutManager = LinearLayoutManager(activity)
        recyclerView?.adapter = alarmsAdapter
        recyclerView?.isNestedScrollingEnabled = false
        alarmsEnabledSwitch = view?.findViewById(R.id.alarms_enabled_switch)
        errorLoadingAlarmsView = view?.findViewById(R.id.unknown_error_view)
        loadingView = view?.findViewById(R.id.loading_view)
        enableAlarmsCaption = view?.findViewById(R.id.enable_alarms_caption)

        alarmsEnabledSwitch?.setOnCheckedChangeListener(object : CompoundButton.OnCheckedChangeListener{

            override fun onCheckedChanged(p0: CompoundButton?, p1: Boolean) {
                if(p1)
                    presenter?.enableAlarms()
                else
                    presenter?.disableAlarms()
            }
        })
        return view!!
    }

    override fun onAlarmAdded(alarm: Alarm?) {
        presenter?.refreshAlarms();
    }

    override fun onPause() {
        super.onPause()
        if (presenter!=null) {
            presenter?.currCarGot = false;
            presenter?.carId = 0
        }
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated");
        setAlarmsEnabled(true)
        presenter?.onUpdateNeeded()
    }

    override fun populateAlarms() {
        Log.d(TAG, "poppulateAlarms");
        if (activity == null) return
        alarmsAdapter?.isDealershipMercedes = isDealershipMercedes;
        alarmsAdapter?.setAlarmList(LinkedHashMap(presenter?.alarmsMap))
        alarmsAdapter?.notifyDataSetChanged()
    }

    override fun noAlarmsView() {
        Log.d(TAG, "noAlarmsVIew()")
        if (activity == null) return
        errorLoadingAlarmsView?.visibility = View.GONE
        loadingView?.visibility = View.GONE
        recyclerView?.visibility = View.GONE
        noALarmsView?.visibility = View.VISIBLE
    }

    override fun showAlarmsView() {
        if (activity == null) return
        errorLoadingAlarmsView?.visibility = View.GONE
        loadingView?.visibility = View.GONE
        noALarmsView?.visibility = View.GONE
        recyclerView?.visibility = View.VISIBLE
    }

    override fun setAlarmsEnabled(alarmsEnabled: Boolean){
        Log.d(TAG,"setAlarmsEnabled $alarmsEnabled")
        if (activity == null) return
        if (alarmsEnabled)
            enableAlarmsCaption!!.text = getString(R.string.alarms_enabled_caption)
        else
            enableAlarmsCaption!!.text = getString(R.string.alarms_disabled_caption)
        if (alarmsEnabledSwitch?.isChecked != alarmsEnabled) {
            alarmsEnabledSwitch?.isChecked = alarmsEnabled
        }
    }

    override fun errorLoadingAlarms() {
        if (activity == null) return
        loadingView?.visibility = View.GONE
        noALarmsView?.visibility = View.GONE
        recyclerView?.visibility = View.GONE
        errorLoadingAlarmsView?.visibility = View.VISIBLE
    }

    override fun showLoading(){
        Log.d(TAG,"showLoading()")
        if (activity == null) return
        loadingView?.visibility = View.VISIBLE
    }

    override fun hideLoading(){
        if (activity == null) return
        Log.d(TAG,"hideLoading()")
        loadingView?.visibility = View.GONE
    }

    override fun toast(message: String) {
        if (activity == null) return
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
    }

    override fun onAlarmClicked(alarm: Alarm) {
        if (activity == null) return
        (activity as AlarmsActivity).switchToAlarmsDescriptionFragment(alarm.name)
    }

    fun serviceUnbinded() {
        alarmsObservable = null;
    }
}
