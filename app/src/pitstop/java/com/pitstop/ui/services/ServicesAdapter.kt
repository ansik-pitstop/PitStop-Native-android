package com.pitstop.ui.services

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import com.pitstop.ui.services.current.CurrentServicesFragment
import com.pitstop.ui.services.history.HistoryServicesFragment
import com.pitstop.ui.services.upcoming.UpcomingServicesFragment

//Return data associated with fragment of the provided tab
class ServicesAdapter(fm: FragmentManager, private val upcomingServicesFragment: UpcomingServicesFragment
                      , private val currentServicesFragment: CurrentServicesFragment
                      , private val historyServicesFragment: HistoryServicesFragment) : FragmentPagerAdapter(fm) {

    private val FRAGMENT_UPCOMING = 0
    private val FRAGMENT_CURRENT = 1
    private val FRAGMENT_HISTORY = 2
    private val FRAGMENT_COUNT = 3

    override fun getItem(position: Int): Fragment? {

        //Return respective fragment and set the variable inside outer class for later callback reference
        when (position) {
            FRAGMENT_UPCOMING -> upcomingServicesFragment
            FRAGMENT_CURRENT -> currentServicesFragment
            FRAGMENT_HISTORY -> historyServicesFragment
        }
        return null
    }

    override fun getCount(): Int {
        return FRAGMENT_COUNT
    }
}