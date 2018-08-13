package com.pitstop.database

import android.support.test.InstrumentationRegistry
import com.pitstop.SensorDataTestUtil
import junit.framework.Assert
import org.junit.Before
import org.junit.Test

/**
 * Created by Karol Zdebel on 8/8/2018.
 */
class LocalPidStorageTest {

    private lateinit var localPidStorage: LocalPidStorage

    @Before
    fun setup(){
        val context = InstrumentationRegistry.getTargetContext()
        localPidStorage = LocalPidStorage(LocalDatabaseHelper.getInstance(context))
    }

    @Test
    fun pidStorageTest(){
        localPidStorage.deleteAllRows()
        val data1 = SensorDataTestUtil.get215PidData(5,"deviceId",1)

        data1.forEach{
            Assert.assertEquals(localPidStorage.store(it),10)
        }
        Assert.assertEquals(localPidStorage.getAllSync(1000).size,50)
    }
}