package com.pitstop.ui.alarms

import android.app.Fragment
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.pitstop.R

/**
 * Created by ishan on 2017-11-03.
 */
class AlarmDescriptionFragment: Fragment() {

    private var TAG: String = javaClass.simpleName;
    private var yourValueTextView : TextView? = null
    private var yourDeviceThreshold: TextView? = null
    private var alarmDescription: TextView? =null
    private var alarmProposal : TextView? =null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {
        Log.d(TAG, "onCreateView()" )
        val view : View?  = inflater?.inflate(R.layout.fragment_alarms, null)
        yourValueTextView = view?.findViewById(R.id.alarm_value)
        yourDeviceThreshold = view?.findViewById(R.id.alarm_threshold)
        alarmDescription = view?.findViewById()

    }
}