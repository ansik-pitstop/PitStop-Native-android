package com.pitstop.ui.graph_pid

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
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
    private lateinit var graphViewMap: MutableMap<String, GraphView>

        override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_pids_graphs)
        presenter = PidGraphsPresenter(PidRepository(LocalPidStorage(LocalDatabaseHelper.getInstance(applicationContext))))
        lineGraphSeriesMap = mutableMapOf()
        graphViewMap = mutableMapOf()

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

    override fun drawGraph(title: String): Boolean {
        Log.d(tag,"drawGraph() title: $title")

        if (graphViewMap[title] != null || lineGraphSeriesMap[title] != null) return false

        val textView = TextView(this)
        textView.text = title
        textView.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
        val textViewLayoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT
                ,LinearLayout.LayoutParams.WRAP_CONTENT)
        textViewLayoutParams.topMargin = Math.round(resources.displayMetrics.density*10)
        textView.layoutParams = textViewLayoutParams

        val graphView = GraphView(this)
        graphView.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT
                , Math.round(resources.displayMetrics.density*100))

        graph_container.addView(textView)
        graph_container.addView(graphView)

        val lineGraphSeries = LineGraphSeries<DataPoint>()
        graphView.addSeries(lineGraphSeries)

        lineGraphSeriesMap[title] = lineGraphSeries
        graphViewMap[title] = graphView

        return true
    }

    override fun addDataPoint(title: String, dataPoint: DataPoint) {
        Log.d(tag,"addDataPoints() title: $title, dataPoint: $dataPoint")
        lineGraphSeriesMap[title]?.appendData(dataPoint,true,1000)
    }

}