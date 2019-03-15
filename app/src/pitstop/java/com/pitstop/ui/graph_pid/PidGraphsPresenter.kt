package com.pitstop.ui.graph_pid

import android.util.Log
import com.jjoe64.graphview.series.DataPoint
import com.pitstop.models.PidGraphDataPoint
import com.pitstop.repositories.PidRepository
import com.pitstop.utils.PIDParser
import java.sql.Date

/**
 * Created by Karol Zdebel on 8/7/2018.
 */
class PidGraphsPresenter(private val pidRepository: PidRepository) {

    private val tag = PidGraphsPresenter::class.java.simpleName
    private var view: PidGraphsView? = null
    private var displayedData = arrayListOf<PidGraphDataPoint>()
    private var pidTypesDisplayed = arrayListOf<String>()

    fun subscribe(view: PidGraphsView){
        this.view = view
    }

    fun unsubscribe(){
        this.view = null
    }

    fun onViewReady(){

        pidRepository.getAll(System.currentTimeMillis() - 1000*60*2)
                .subscribe {
                    Log.d(tag,"Got data: $it")
                    it.forEach {
                        val title = PIDParser.getPidName(it.type)
                        if (!pidTypesDisplayed.contains(title)){
                            pidTypesDisplayed.add(title)
                            view?.drawGraph(title)
                        }
                        if (!displayedData.contains(it)){
                            Log.d(tag,"undisplayed point: $it")
                            view?.addDataPoint(title
                                    ,DataPoint(Date(it.timestamp),it.value.toDouble()))
                            view?.displayCurrentPidValue(title,it.value.toString())

                            displayedData.add(it)
                        }
                    }
                }
    }
}