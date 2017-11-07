package com.pitstop.ui.alarms

import android.app.FragmentManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import android.view.WindowManager
import com.pitstop.R
import com.pitstop.models.Alarm
import com.pitstop.ui.vehicle_specs.VehicleSpecsActivity
import com.pitstop.ui.vehicle_specs.VehicleSpecsFragment

/**
 * Created by ishan on 2017-10-30.
 */
class AlarmsActivity: AppCompatActivity() {

    val TAG = VehicleSpecsActivity::class.java.simpleName
    var alarmsFragment: AlarmsFragment?  = null
    var bundle: Bundle? = Bundle()
    var alarmClicked: Alarm? = null;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarms)
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

        fragmentTransaction.replace(R.id.alarms_fragment_holder, alarmsFragment)
        fragmentTransaction.commit()
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        onBackPressed()
        return true
    }


}