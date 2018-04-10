package com.pitstop.ui.Notifications

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.pitstop.R

/**
 * Created by Karol Zdebel on 4/10/2018.
 */
class NotificationsActivity: AppCompatActivity() {

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
}