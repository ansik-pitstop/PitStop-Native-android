package com.pitstop.ui.alarms

import android.app.Activity
import android.app.Fragment
import android.bluetooth.BluetoothAdapter
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.pitstop.R
import com.pitstop.adapters.AlarmsAdapter
import com.pitstop.application.GlobalApplication
import com.pitstop.bluetooth.BluetoothAutoConnectService
import com.pitstop.dependency.ContextModule
import com.pitstop.dependency.DaggerUseCaseComponent
import com.pitstop.models.Alarm
import com.pitstop.observer.AlarmObservable
import com.pitstop.observer.AlarmObserver
import com.pitstop.ui.dashboard.DashboardFragment
import com.pitstop.ui.my_garage.MyGaragePresenter
import com.pitstop.utils.AnimatedDialogBuilder
import com.pitstop.utils.MixpanelHelper

/**
 * Created by ishan on 2017-10-30.
 */
class AlarmsFragment : AlarmsView, Fragment(), AlarmObserver {
    val TAG: String = javaClass.simpleName

    var presenter : AlarmsPresenter? = null
    var recyclerView :RecyclerView? = null
    var noALarmsView : View? = null
    var alarmsAdapter:AlarmsAdapter?  =null
    var alarmsObservable :AlarmObservable? = null
    var autoConnectService : BluetoothAutoConnectService? = null;

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            Log.d(TAG, "serviceConnection.onServiceConnected()")
            autoConnectService =  ((service as BluetoothAutoConnectService.BluetoothBinder)
                    .service)

            alarmsObservable = ((service as BluetoothAutoConnectService.BluetoothBinder)
                    .service) as AlarmObservable
            autoConnectService?.subscribe(this@AlarmsFragment)

        }

        override fun onServiceDisconnected(name: ComponentName) {
            Log.d(TAG, "serviceConnection.onServiceDisconnected()")
            alarmsObservable = null
            autoConnectService = null

        }
    }
    override fun onAlarmAdded(alarm: Alarm?) {
        presenter?.onUpdateNeeded();
    }

    override fun onPause() {
        super.onPause()
        if (presenter!=null) {
            presenter?.currCarGot = false;
            presenter?.carId = null
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {
        Log.d(TAG, "onCreateView()" )
        val view : View?  = inflater?.inflate(R.layout.fragment_alarms, null)
       activity.bindService(Intent(activity, BluetoothAutoConnectService::class.java),
                serviceConnection, Context.BIND_AUTO_CREATE)

        if (presenter == null) {
            val useCaseComponent = DaggerUseCaseComponent.builder()
                    .contextModule(ContextModule(activity))
                    .build()
            val mixpanelHelper = MixpanelHelper(activity
                    .applicationContext as GlobalApplication)
            presenter = AlarmsPresenter(useCaseComponent, mixpanelHelper)

        }
        presenter?.subscribe(this)
        noALarmsView = view?.findViewById(R.id.no_alarms_view)
        recyclerView = view?.findViewById(R.id.main_recycler_view)
        alarmsAdapter = AlarmsAdapter(presenter?.alarmsMap)
        recyclerView?.layoutManager = LinearLayoutManager(activity)
        recyclerView?.adapter = alarmsAdapter

        return view!!
    }


    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated");
        presenter?.onUpdateNeeded()
    }



    override fun populateAlarms(isDealershipMercedes: Boolean) {
        Log.d(TAG, "poppulateAlarms");
        alarmsAdapter?.isDealershipMercedes = isDealershipMercedes;
        alarmsAdapter?.notifyDataSetChanged()

    }

    override fun noAlarmsView() {
        recyclerView?.visibility = View.GONE
        noALarmsView?.visibility = View.VISIBLE
    }

    override fun showAlarmsView() {
        noALarmsView?.visibility = View.GONE
        recyclerView?.visibility = View.VISIBLE
    }

}
