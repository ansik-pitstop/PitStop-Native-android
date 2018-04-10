package com.pitstop.ui.Notifications

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.pitstop.R
import com.pitstop.ui.main_activity.TabSwitcher

/**
 * Created by Karol Zdebel on 4/10/2018.
 */
class NotificationsActivity: AppCompatActivity(), TabSwitcher {

    companion object{
        const val GO_TO_NONE = 0
        const val GO_TO_SERVICES = 1
        const val GO_TO_SCAN = 2
        const val GO_TO_APPOINTMENTS = 3
    }

    private lateinit var notificationFragment: NotificationFragment

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)
        notificationFragment = NotificationFragment()
    }

    override fun onStart() {
        super.onStart()
        supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, notificationFragment)
                .commit()
    }

    override fun openCurrentServices() {
        setResult(GO_TO_SERVICES)
        finish()
    }

    override fun openAppointments() {
        setResult(GO_TO_APPOINTMENTS)
        finish()
    }

    override fun openScanTab() {
        setResult(GO_TO_SCAN)
        finish()
    }
}