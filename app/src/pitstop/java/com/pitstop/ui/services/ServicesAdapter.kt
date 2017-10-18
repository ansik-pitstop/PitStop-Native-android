package com.pitstop.ui.services

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import com.pitstop.ui.services.current.CurrentServicesFragment
import com.pitstop.ui.services.history.HistoryServicesFragment
import com.pitstop.ui.services.upcoming.UpcomingServicesFragment

class ServicesTabAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

    private val fragmentUpcoming = 0
    private val fragmentCurrent = 1
    private val fragmentHistory = 2
    private val fragmentCount = 3

    override fun getItem(position: Int): Fragment? {

        //Return respective fragment and set the variable inside outer class for later callback reference
        when (position) {
            fragmentUpcoming -> return UpcomingServicesFragment()
            fragmentCurrent -> return CurrentServicesFragment()
            fragmentHistory -> return HistoryServicesFragment()
        }
        return null
    }

    override fun getCount(): Int {
        return fragmentCount
    }
}