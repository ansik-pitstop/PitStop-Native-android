package com.pitstop.ui.services

import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.pitstop.R
import com.pitstop.dependency.ContextModule
import com.pitstop.dependency.DaggerUseCaseComponent
import com.pitstop.ui.services.upcoming.MainServicesView

class MainServicesFragment : Fragment(), MainServicesView {

    private var servicesPager: SubServiceViewPager? = null
    private var tabLayout: TabLayout? = null
    private var presenter: MainServicesPresenter? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        servicesPager = activity.findViewById<View>(R.id.services_viewpager) as SubServiceViewPager?
        servicesPager?.offscreenPageLimit = 2

        //Create tab layout
        val tabLayout = activity.findViewById<View>(R.id.tab_layout) as TabLayout?
        this.tabLayout = tabLayout
        if (tabLayout != null){
            tabLayout.addTab(tabLayout.newTab().setText("Upcoming"))
            tabLayout.addTab(tabLayout.newTab().setText("Current"))
            tabLayout.addTab(tabLayout.newTab().setText("History"))
            tabLayout.tabGravity = TabLayout.GRAVITY_FILL
            tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    servicesPager?.currentItem = tab.position
                }

                override fun onTabUnselected(tab: TabLayout.Tab) {

                }

                override fun onTabReselected(tab: TabLayout.Tab) {

                }
            })
        }


        servicesPager?.adapter = ServicesTabAdapter(childFragmentManager)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val rootView = inflater!!.inflate(R.layout.fragment_services, null)

        if (presenter == null){
            val useCaseComponent = DaggerUseCaseComponent.builder()
                    .contextModule(ContextModule(context.applicationContext))
                    .build()
            presenter = MainServicesPresenter(useCaseComponent)

        }
        return rootView
    }

    override fun onResume() {
        super.onResume()
        presenter?.subscribe(this)
        presenter?.populateUI()

    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter?.unsubscribe()
    }

    override fun bindDefaultDealerUI() {

        //Get the themes default primary color
        val defaultColor = TypedValue()
        if (activity == null) {
            return
        }
        activity.theme.resolveAttribute(android.R.attr.colorPrimary, defaultColor, true)

        //Set other changed UI elements back to original color
        tabLayout?.setBackgroundColor(defaultColor.data)
    }

    override fun bindMercedesDealerUI() {
        tabLayout?.setBackgroundColor(Color.BLACK)
    }

    fun setCurrent() {
        if (servicesPager == null) {
            return
        }
        servicesPager?.currentItem = 1
    }

    companion object {

        fun newInstance(): MainServicesFragment {
            return MainServicesFragment()
        }
    }

}
