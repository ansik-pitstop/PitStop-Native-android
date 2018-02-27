package com.pitstop.database

import android.content.Context
import android.util.Log
import com.pitstop.models.Trip

/**
 * Created by Karol Zdebel on 2/27/2018.
 */
class LocalGpsTripStorage(val context: Context) {

    val tag = javaClass.simpleName

    fun store(trip: Trip){
        Log.d(tag,"store() trip: "+trip)
        //Todo: IMPLEMENT THIS
    }

    fun remove(trip: Trip){
        Log.d(tag,"remove() trip: "+trip)
        //Todo: IMPLEMENT THIS
    }

    fun getAll(): List<Trip>{
        Log.d(tag,"getAll()")
        //Todo: IMPLEMENT THIS
        return emptyList()
    }
}