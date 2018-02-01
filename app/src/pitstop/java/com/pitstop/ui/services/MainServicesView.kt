package com.pitstop.ui.services

/**
 * Created by Karol Zdebel on 2/1/2018.
 */

interface MainServicesView {
    enum class ServiceTab(val tabNum: Int) {
        CURRENT(1), UPCOMING(0), HISTORY(2)
    }

    //Select respective tab
    fun selectTab(tab: ServiceTab)
}
