package com.pitstop.ui.notifications

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.pitstop.R
import com.pitstop.ui.main_activity.TabSwitcher

/**
 * Created by Karol Zdebel on 4/10/2018.
 */
class NotificationsActivity: AppCompatActivity(), TabSwitcher {

    private val tag = NotificationsActivity::class.java.simpleName

    companion object{
        const val GO_TO_NONE = 0
        const val GO_TO_SERVICES = 1
        const val GO_TO_SCAN = 2
        const val GO_TO_APPOINTMENTS = 3
        const val GO_TO_REQUEST_SERVICE = 4
        const val GO_TO_SMOOCH_MESSAGES = 6
    }

    private lateinit var notificationFragment: NotificationFragment

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)
        notificationFragment = com.pitstop.ui.notifications.NotificationFragment()
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.d(tag, "onOptionsItemSelected()")
        return if (item.itemId == android.R.id.home) {
            onBackPressed()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    override fun onStart() {
        super.onStart()
        try{
            supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, notificationFragment)
                    .commit()
        }catch(e: RuntimeException){
            //Already added
        }

    }

    override fun openCurrentServices() {
        Log.d(tag,"openCurrentServices()")
        setResult(GO_TO_SERVICES)
        finish()
    }

    override fun openAppointments() {
        Log.d(tag,"openAppointments()")
        setResult(GO_TO_APPOINTMENTS)
        finish()
    }

    override fun openScanTab() {
        Log.d(tag,"openScanTab()")
        setResult(GO_TO_SCAN)
        finish()
    }

    override fun openRequestService() {
        Log.d(tag,"openRequestService()")
        setResult(GO_TO_REQUEST_SERVICE)
        finish()
    }
}