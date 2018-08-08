package com.pitstop.ui.graph_pid

import android.util.Log
import com.jjoe64.graphview.series.DataPoint
import com.pitstop.models.PidGraphDataPoint
import com.pitstop.repositories.PidRepository

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
        pidRepository.getAll(System.currentTimeMillis() - 1000*60*60)
                .subscribe({
                    Log.d(tag,"Got data: $it")
                    it.forEach {
                        if (!pidTypesDisplayed.contains(it.type)){
                            pidTypesDisplayed.add(it.type)
                            view?.drawGraph(it.type)
                        }
                        if (!displayedData.contains(it)){
                            view?.addDataPoint(it.type
                                    ,DataPoint((displayedData.size+1).toDouble(),it.value.toDouble()))
                            displayedData.add(it)
                        }
                    }
                })
    }
}