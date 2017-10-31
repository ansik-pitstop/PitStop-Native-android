package com.pitstop.ui.alarms

import android.app.FragmentManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import com.pitstop.R
import com.pitstop.ui.vehicle_specs.VehicleSpecsActivity
import com.pitstop.ui.vehicle_specs.VehicleSpecsFragment

/**
 * Created by ishan on 2017-10-30.
 */
class AlarmsActivity: AppCompatActivity() {

    val TAG = VehicleSpecsActivity::class.java.simpleName
    var alarmsFragment: AlarmsFragment?  = null
    private var bundle: Bundle? = Bundle()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarms)
        alarmsFragment = AlarmsFragment()
        alarmsFragment?.arguments = bundle
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.alarms_fragment_holder, alarmsFragment)
        fragmentTransaction.commit()
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        onBackPressed()
        return true
    }


}