package com.pitstop.ui.graph_pid

import android.util.Log
import com.pitstop.repositories.PidRepository

/**
 * Created by Karol Zdebel on 8/7/2018.
 */
class PidGraphsPresenter(private val pidRepository: PidRepository) {

    private val tag = PidGraphsActivity::class.java.simpleName
    private var view: PidGraphsView? = null

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
                })
    }
}