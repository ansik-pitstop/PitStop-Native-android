package com.pitstop.ui.alarms

import android.app.Fragment
import android.os.Bundle
import android.util.Log
import android.util.Range
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.pitstop.R
import com.pitstop.adapters.AlarmsAdapter
import com.pitstop.models.Alarm

/**
 * Created by ishan on 2017-11-03.
 */
class AlarmDescriptionFragment: Fragment() {

    private var TAG: String = javaClass.simpleName;
    private var alarmEventName : TextView? = null
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
        alarmEventName = view?.findViewById(R.id.alarm_event_name)
        yourDeviceThreshold = view?.findViewById(R.id.alarm_threshold)
        alarmDescription = view?.findViewById(R.id.alarms_description)
        alarmProposal = view?.findViewById(R.id.alarm_proposal)
        descriptionLabel = view?.findViewById(R.id.description_label)
        proposalLabel = view?.findViewById(R.id.proposal_label)

        if ((activity as AlarmsActivity).alarmClicked!=null){
            var alarm: Alarm = (activity as AlarmsActivity).alarmClicked!!
            yourValueTextView?.text = (alarm.alarmValue.toString() + getAlarmValueUnits(alarm));
            alarmEventName?.text = AlarmsAdapter.getAlarmName(alarm.alarmEvent)
            alarmDescription?.text = getAlarmDescription(alarm);
            alarmProposal?.text = getAlarmProposal(alarm)
            if (getAlarmProposal(alarm).equals("", true))
                proposalLabel?.visibility = View.GONE
            if (getAlarmDescription(alarm).equals("", true))
                descriptionLabel?.visibility = View.GONE
            yourDeviceThreshold?.text = getDeviceThreshold(alarm)
        }
        if((activity as AlarmsActivity).alarmClicked?.alarmEvent in (0..2) ||(activity as AlarmsActivity).alarmClicked?.alarmEvent in (10..11) ){
            yourValueTextView?.visibility = View.GONE
            yourDeviceThreshold?.visibility = View.GONE
            view?.findViewById<TextView>(R.id.value_label)?.visibility = View.GONE
            view?.findViewById<TextView>(R.id.threshold_label)?.visibility = View.GONE

        }
        return view!!
    }

    private fun getAlarmValueUnits(alarm: Alarm): Any? {
        return when(alarm.alarmEvent){
            3-> " C"
            4-> "RPM"
            5 ->"Volts"
            6,7-> "minutes"
            8->"km/h"
            9,12,13,14,15,16->"g"
            else -> ""
        }
    }



    fun getAlarmDescription(alarm: Alarm): String{
        val alarmEvent:Int = alarm.alarmEvent

        var returnString:String = when(alarmEvent){
            0->"Power on"
            1->getString(R.string.ignition_on_desc)
            2->getString(R.string.ignition_off_desc)
            3->"Engine Coolant Over Temperature"
            4-> getString(R.string.high_rpm_desc)
            5-> getString(R.string.low_voltage_desc)
            6->getString(R.string.idling_desc)
            7->getString(R.string.fatigue_driving_desc)
            8-> ""
            9->getString(R.string.collision_desc)
            10->getString(R.string.shock)
            11->getString(R.string.towing)
            12->getString(R.string.dangerous_driving)
            13-> ""
            14->getString(R.string.decceleration_desc)
            15->getString(R.string.sharp_turn_description)
            16->getString(R.string.quick_lane_change_desc)
            else-> ""

        }
        return returnString;
    }

    fun getAlarmProposal(alarm: Alarm) :String{
        return when(alarm.alarmEvent) {
            0 -> "Power on"
            1 -> ""
            2 -> ""
            3 -> "Engine Coolant Over Temperature"
            4 -> ""
            5 -> getString(R.string.low_voltage_proposal)
            6 -> getString(R.string.idling_proposal)
            7 -> getString(R.string.fatigue_driving_proposal)
            8 -> getString(R.string.speeding_desc)
            9 -> getString(R.string.collision_desc)
            10 -> getString(R.string.shock)
            11 -> getString(R.string.towing)
            12 -> getString(R.string.dangerous_driving)
            13 -> getString(R.string.high_rpm_desc)
            14 -> getString(R.string.decceleration_proposal)
            15 -> getString(R.string.sharp_turn_proposal)
            16 -> getString(R.string.quick_lane_change_proposal)
            else -> ""
        }

    }


    fun getDeviceThreshold(alarm: Alarm): String{
        return when(alarm.alarmEvent) {
            3 -> "98 C"
            4 -> "4000 RPM"
            5 -> "11.5 Volts"
            6 -> "15 minutes"
            7 -> "240 minutes"
            8 -> "100/km/h"
            9 -> "1.5 G"
            12 -> "0.5 G"
            13,15 ->"0.4 G"
            14,16 -> "0.6 G"

            else -> ""
        }
    }




}