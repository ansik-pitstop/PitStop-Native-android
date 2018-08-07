package com.pitstop.ui.graph_pid

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import com.pitstop.R
import com.pitstop.database.LocalDatabaseHelper
import com.pitstop.database.LocalPidStorage
import com.pitstop.repositories.PidRepository
import kotlinx.android.synthetic.main.activity_pids_graphs.*

/**
 * Created by Karol Zdebel on 8/7/2018.
 */
class PidGraphsActivity: AppCompatActivity(), PidGraphsView {

    private val tag = PidGraphsActivity::class.java.simpleName

    private var presenter: PidGraphsPresenter? = null
    private lateinit var lineGraphSeriesMap: MutableMap<String, LineGraphSeries<DataPoint>>

        override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_pids_graphs)
        presenter = PidGraphsPresenter(PidRepository(LocalPidStorage(LocalDatabaseHelper.getInstance(applicationContext))))
        lineGraphSeriesMap = mutableMapOf()

        super.onCreate(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        presenter?.subscribe(this)
        presenter?.onViewReady()
    }

    override fun onStop() {
        super.onStop()
        presenter?.unsubscribe()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun displaySeriesData(series: String, dataPoint: DataPoint) {
        Log.d(tag, "displaySeriesData() series: $series, coordinate:$dataPoint")
        var lineGraphSeries: LineGraphSeries<DataPoint>? = lineGraphSeriesMap[series]
        if (lineGraphSeries == null) {
            lineGraphSeries = LineGraphSeries()
            lineGraphSeriesMap[series] = lineGraphSeries
            val graph: GraphView = graph_1
            graph.addSeries(lineGraphSeries)
        }

        lineGraphSeries.appendData(dataPoint, true, 40)
    }
}