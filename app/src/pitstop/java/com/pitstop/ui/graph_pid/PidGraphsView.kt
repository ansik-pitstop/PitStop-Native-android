package com.pitstop.ui.graph_pid

import com.jjoe64.graphview.series.DataPoint

/**
 * Created by Karol Zdebel on 8/7/2018.
 */
interface PidGraphsView {
    fun drawGraph(title: String): Boolean
    fun addDataPoint(title: String, dataPoint: DataPoint)
    fun displayCurrentPidValue(title: String, value: String)
}