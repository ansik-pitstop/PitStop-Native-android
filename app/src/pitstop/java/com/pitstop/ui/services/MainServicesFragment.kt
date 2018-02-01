package com.pitstop.ui.services

import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.pitstop.R
import com.pitstop.application.GlobalApplication
import com.pitstop.dependency.ContextModule
import com.pitstop.dependency.DaggerUseCaseComponent
import com.pitstop.utils.MixpanelHelper

class MainServicesFragment : Fragment(), MainServicesView {

    private val TAG = MainServicesFragment::class.java.simpleName

    private var servicesPager: SubServiceViewPager? = null
    private var tabLayout: TabLayout? = null
    private var presenter: MainServicesPresenter? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val rootview = inflater!!.inflate(R.layout.fragment_services, null)
        servicesPager = activity.findViewById(R.id.services_viewpager)

        if (presenter == null){
            val usecaseComponent = DaggerUseCaseComponent.builder()
                    .contextModule(ContextModule(context.applicationContext))
                    .build()
            presenter = MainServicesPresenter(usecaseComponent
                    , MixpanelHelper(context.applicationContext as GlobalApplication))

        }
        presenter!!.subscribe(this)

        return rootview
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (presenter != null)
            presenter!!.unsubscribe()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        servicesPager = activity.findViewById<View>(R.id.services_viewpager) as SubServiceViewPager
        servicesPager!!.offscreenPageLimit = 2

        //Create tab layout
        tabLayout = activity.findViewById<View>(R.id.tab_layout) as TabLayout
        tabLayout!!.addTab(tabLayout!!.newTab().setText(getString(R.string.upcoming_services)))
        tabLayout!!.addTab(tabLayout!!.newTab().setText(getString(R.string.current_services)))
        tabLayout!!.addTab(tabLayout!!.newTab().setText(getString(R.string.history_services)))
        tabLayout!!.tabGravity = TabLayout.GRAVITY_FILL
        tabLayout!!.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                servicesPager!!.currentItem = tab.position
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {

            }

            override fun onTabReselected(tab: TabLayout.Tab) {

            }
        })

        servicesPager!!.adapter = ServicesAdapter(childFragmentManager)
        tabLayout!!.getTabAt(1)!!.select()

    }

    override fun selectTab(tab: MainServicesView.ServiceTab) {
        Log.d(TAG,"selectTab() tabNum: "+tab.tabNum);
        if (servicesPager == null) {
            return
        }
        tabLayout!!.getTabAt(tab.tabNum)!!.select()
    }
}
