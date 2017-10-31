package com.pitstop.ui.alarms

import android.app.Fragment
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.pitstop.R
import com.pitstop.adapters.AlarmsAdapter
import com.pitstop.application.GlobalApplication
import com.pitstop.dependency.ContextModule
import com.pitstop.dependency.DaggerUseCaseComponent
import com.pitstop.models.Alarm
import com.pitstop.ui.dashboard.DashboardFragment
import com.pitstop.ui.my_garage.MyGaragePresenter
import com.pitstop.utils.MixpanelHelper

/**
 * Created by ishan on 2017-10-30.
 */
class AlarmsFragment : AlarmsView, Fragment() {
    val TAG: String = javaClass.simpleName;

    var presenter : AlarmsPresenter? = null
    var recyclerView :RecyclerView? = null
    var alarmsAdapter:AlarmsAdapter?  =null


    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {
        Log.d(TAG, "onCreateView()" );
        val view : View?  = inflater?.inflate(R.layout.fragment_alarms, null)
        if (presenter == null) {
            val useCaseComponent = DaggerUseCaseComponent.builder()
                    .contextModule(ContextModule(activity))
                    .build()
            val mixpanelHelper = MixpanelHelper(activity
                    .applicationContext as GlobalApplication)
            presenter = AlarmsPresenter(useCaseComponent, mixpanelHelper)

        }
        presenter?.subscribe(this)
        recyclerView = view?.findViewById(R.id.main_recycler_view);
        alarmsAdapter = AlarmsAdapter(presenter?.alarmList);
        recyclerView?.layoutManager = LinearLayoutManager(activity);
        recyclerView?.adapter = alarmsAdapter;

        return view!!
    }


    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated");
        presenter?.onUpdateNeeded()
    }

    override fun noCarView() {
        // TODO
    }

    override fun populateAlarms() {
        Log.d(TAG, "poppulateAlarms");
        alarmsAdapter?.notifyDataSetChanged()

    }

    override fun errorView() {
        //TODO
    }
}

