package com.pitstop.ui.alarms

import android.app.Fragment
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.pitstop.R
import com.pitstop.models.Alarm

/**
 * Created by ishan on 2017-11-03.
 */
class AlarmDescriptionFragment: Fragment() {

    private var TAG: String = javaClass.simpleName;
    private var yourValueTextView : TextView? = null
    private var yourDeviceThreshold: TextView? = null
    private var alarmDescription: TextView? =null
    private var alarmProposal : TextView? =null
    private var descriptionLabel: TextView? = null
    private var proposalLabel: TextView? = null


    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {
        Log.d(TAG, "onCreateView()" )
        val view : View?  = inflater?.inflate(R.layout.fragment_alarm_description, null)
        yourValueTextView = view?.findViewById(R.id.alarm_value)
        yourDeviceThreshold = view?.findViewById(R.id.alarm_threshold)
        alarmDescription = view?.findViewById(R.id.alarms_description)
        alarmProposal = view?.findViewById(R.id.alarm_proposal)
        descriptionLabel = view?.findViewById(R.id.description_label)
        proposalLabel = view?.findViewById(R.id.proposal_label)

        if ((activity as AlarmsActivity).alarmClicked!=null){
            var alarm: Alarm = (activity as AlarmsActivity).alarmClicked!!
            yourValueTextView?.text = (alarm.value.toString() + alarm.valueUnits)
            alarmDescription?.text = alarm.description(activity)
            alarmProposal?.text = alarm.proposal(activity)
            if (alarm.proposal(activity).equals("", true))
                proposalLabel?.visibility = View.GONE
            if (alarm.description(activity).equals("", true))
                descriptionLabel?.visibility = View.GONE
            yourDeviceThreshold?.text = alarm.threshold
        }
        if((activity as AlarmsActivity).alarmClicked?.event in (0..2) ||(activity as AlarmsActivity).alarmClicked?.event in (10..11) ){
            yourValueTextView?.visibility = View.GONE
            yourDeviceThreshold?.visibility = View.GONE
            view?.findViewById<TextView>(R.id.value_label)?.visibility = View.GONE
            view?.findViewById<TextView>(R.id.threshold_label)?.visibility = View.GONE

        }
        return view!!
    }
}