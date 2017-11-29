package com.pitstop.models

import android.content.Context
import com.pitstop.R

/**val
 * Created by ishan on 2017-10-30.
 */
class Alarm(val event: Int, val value: Float, val rtcTime: String, var carID : Int?){
    val name: String = when (event) {
       0 -> "Power On Alarm"
       1 -> "Ignition On Alarm"
       2 -> "Ignition Off Alarm"
       3 -> "Engine Coolant Over Temperature"
       4 -> "High RPM"
       5 -> "Low Voltage"
       6 -> "Idling"
       7 -> "Fatigue Driving"
       8 -> "Speeding"
       9 -> "Collision"
       10 -> "Shock"
       11 -> "Towing"
       12 -> "Dangerous Driving"
       13 -> "Acceleration"
       14 -> "Deceleration"
       15 -> "Sharp Turn"
       16 -> "Quick Lane Change"
       else -> ""
    }

    val valueUnits: String = when(event){
        3-> " C"
        4-> "RPM"
        5 ->"Volts"
        6,7-> "minutes"
        8->"km/h"
        9,12,13,14,15,16->"g"
        else -> ""
    }

    val threshold: String = when(event) {
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

    fun proposal(context: Context): String = when(event) {
        0 -> "Power on"
        1 -> ""
        2 -> ""
        3 -> "Engine Coolant Over Temperature"
        4 -> ""
        5 -> context.getString(R.string.low_voltage_proposal)
        6 -> context.getString(R.string.idling_proposal)
        7 -> context.getString(R.string.fatigue_driving_proposal)
        8 -> context.getString(R.string.speeding_desc)
        9 -> context.getString(R.string.collision_desc)
        10 -> context.getString(R.string.shock)
        11 -> context.getString(R.string.towing)
        12 -> context.getString(R.string.dangerous_driving)
        13 -> context.getString(R.string.high_rpm_desc)
        14 -> context.getString(R.string.decceleration_proposal)
        15 -> context.getString(R.string.sharp_turn_proposal)
        16 -> context.getString(R.string.quick_lane_change_proposal)
        else -> ""
    }

    fun description(context: Context): String = when(event){
        0-> "Power on"
        1-> context.getString(R.string.ignition_on_desc)
        2-> context.getString(R.string.ignition_off_desc)
        3-> "Engine Coolant Over Temperature"
        4-> context.getString(R.string.high_rpm_desc)
        5-> context.getString(R.string.low_voltage_desc)
        6-> context.getString(R.string.idling_desc)
        7-> context.getString(R.string.fatigue_driving_desc)
        8-> ""
        9-> context.getString(R.string.collision_desc)
        10-> context.getString(R.string.shock)
        11-> context.getString(R.string.towing)
        12-> context.getString(R.string.dangerous_driving)
        13-> ""
        14-> context.getString(R.string.decceleration_desc)
        15-> context.getString(R.string.sharp_turn_description)
        16-> context.getString(R.string.quick_lane_change_desc)
        else-> ""

    }

}