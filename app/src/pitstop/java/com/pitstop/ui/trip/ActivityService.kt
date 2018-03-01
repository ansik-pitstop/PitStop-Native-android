package com.pitstop.ui.trip

import android.app.IntentService
import android.content.Intent
import android.util.Log


/**
 * Created by Karol Zdebel on 2/27/2018.
 */
class ActivityService: IntentService("ActivityService") {

    companion object {
        val DETECTED_ACTIVITY = "detected_activity"
    }

    override fun onHandleIntent(intent: Intent?) {
        Log.d(javaClass.simpleName,"onHandleIntent()")
        intent?.action = DETECTED_ACTIVITY
        sendBroadcast(intent)
    }
}