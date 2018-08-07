package com.pitstop.repositories

import android.util.Log
import com.pitstop.bluetooth.dataPackages.PidPackage
import com.pitstop.database.LocalPidStorage
import com.squareup.sqlbrite.QueryObservable

/**
 * Created by Karol Zdebel on 8/17/2017.
 */

class PidRepository(private val localPidStorage: LocalPidStorage) : Repository {

    private val TAG = javaClass.simpleName

    fun getAll(after: Long): QueryObservable{
        Log.d(TAG,"getAll() after: $after")
        return localPidStorage.getAll(after)
    }

    fun store(pidPackage: PidPackage): Int{
        Log.d(TAG,"store() pidPackage: $pidPackage")
        return localPidStorage.store(pidPackage)
    }

}
