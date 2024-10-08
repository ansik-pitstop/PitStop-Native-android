package com.pitstop.ui.graph_pid

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import com.pitstop.R
import com.pitstop.database.LocalDatabaseHelper
import com.pitstop.database.LocalPidStorage
import com.pitstop.repositories.PidRepository
import kotlinx.android.synthetic.main.activity_pids_graphs.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by Karol Zdebel on 8/7/2018.
 */
class PidGraphsActivity: AppCompatActivity(), PidGraphsView {

    private val tag = PidGraphsActivity::class.java.simpleName

    private var presenter: PidGraphsPresenter? = null
    private lateinit var lineGraphSeriesMap: MutableMap<String, LineGraphSeries<DataPoint>>
    private lateinit var graphViewMap: MutableMap<String, GraphView>
    private lateinit var textViewMap: MutableMap<String, TextView>

        override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_pids_graphs)
        presenter = PidGraphsPresenter(PidRepository(LocalPidStorage(LocalDatabaseHelper.getInstance(applicationContext))))
        lineGraphSeriesMap = mutableMapOf()
        graphViewMap = mutableMapOf()
        textViewMap = mutableMapOf()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        super.onCreate(savedInstanceState)
    }

//    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
//        return if (item?.itemId == android.R.id.home) {
//            super.onBackPressed()
//            true
//        }
//        else return super.onOptionsItemSelected(item)
//    }

    override fun onStart() {
        super.onStart()
        presenter?.subscribe(this)
        presenter?.onViewReady()
    }

    override fun onStop() {
        super.onStop()
        presenter?.unsubscribe()
    }

    override fun drawGraph(title: String): Boolean {
        Log.d(tag,"drawsGraph() title: $title")

        if (graphViewMap[title] != null || lineGraphSeriesMap[title] != null) return false

        val textView = TextView(this)
        textView.text = title
        textView.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
        val textViewLayoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT
                ,LinearLayout.LayoutParams.WRAP_CONTENT)
        textViewLayoutParams.topMargin = Math.round(resources.displayMetrics.density*10)
        textView.layoutParams = textViewLayoutParams

        val graphView = GraphView(this)
        val dateFormat = SimpleDateFormat("hh:mm:ss", Locale.getDefault())
        graphView.gridLabelRenderer.isHorizontalLabelsVisible = false
        graphView.viewport.isXAxisBoundsManual = true
        graphView.viewport.setMinX(0.0)
        graphView.viewport.setMaxX(80000.0)
        graphView.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT
                , Math.round(resources.displayMetrics.density*100))

        graph_container.addView(textView)
        graph_container.addView(graphView)

        val lineGraphSeries = LineGraphSeries<DataPoint>()
        graphView.addSeries(lineGraphSeries)

        lineGraphSeriesMap[title] = lineGraphSeries
        graphViewMap[title] = graphView
        textViewMap[title] = textView

        return true
    }

    override fun addDataPoint(title: String, dataPoint: DataPoint) {
        Log.d(tag,"addDataPoints() title: $title, dataPoint: $dataPoint")
        lineGraphSeriesMap[title]?.appendData(dataPoint,true,12)
    }

    override fun displayCurrentPidValue(title: String, value: String) {
        Log.d(tag,"displayCurrentPidValue() title: $title, value: $value")
        textViewMap[title]?.text = "$title: $value"
    }

}