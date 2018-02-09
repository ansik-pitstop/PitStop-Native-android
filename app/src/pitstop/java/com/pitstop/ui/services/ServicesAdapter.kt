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

    companion object {
        val FRAGMENT_UPCOMING = 0
        val FRAGMENT_CURRENT = 1
        val FRAGMENT_HISTORY = 2
        val FRAGMENT_COUNT = 3
    }

    override fun getItem(position: Int): Fragment? {

        //Return respective fragment and set the variable inside outer class for later callback reference
        when (position) {
            FRAGMENT_UPCOMING -> return upcomingServicesFragment
            FRAGMENT_CURRENT -> return currentServicesFragment
            FRAGMENT_HISTORY -> return historyServicesFragment
        }
        return null
    }

    override fun getCount(): Int {
        return FRAGMENT_COUNT
    }
}