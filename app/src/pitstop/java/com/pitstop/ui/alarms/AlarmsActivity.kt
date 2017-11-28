package com.pitstop.ui.alarms

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import android.view.WindowManager
import com.pitstop.R
import com.pitstop.bluetooth.BluetoothAutoConnectService
import com.pitstop.models.Alarm

/**
 * Created by ishan on 2017-10-30.
 */
class AlarmsActivity: AppCompatActivity() {

    val TAG = AlarmsActivity::class.java.simpleName
    var alarmsFragment: AlarmsFragment?  = null
    var bundle: Bundle? = Bundle()
    var alarmClicked: Alarm? = null;
    var autoConnectService : BluetoothAutoConnectService? = null;
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            Log.d(TAG, "serviceConnection.onServiceConnected()")
            autoConnectService =  ((service as BluetoothAutoConnectService.BluetoothBinder)
                    .service)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            Log.d(TAG, "serviceConnection.onServiceDisconnected()")
            autoConnectService = null
            alarmsFragment?.serviceUnbinded()

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarms)
        bindService(Intent(this, BluetoothAutoConnectService::class.java),
                serviceConnection, Context.BIND_AUTO_CREATE)
        alarmsFragment = AlarmsFragment()
        alarmsFragment?.arguments = intent.extras;
        val fragmentTransaction = fragmentManager.beginTransaction()
        if(intent.extras.getBoolean("isMercedes")){
            supportActionBar!!.setBackgroundDrawable(ColorDrawable( Color.BLACK ))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                window.statusBarColor = ContextCompat.getColor(this, R.color.black)
            }
        }
        supportActionBar!!.title = "Driving Alarms"

        fragmentTransaction.replace(R.id.alarms_fragment_holder, alarmsFragment)
        fragmentTransaction.commit()
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        onBackPressed()
        return true
    }


}