package com.pitstop.database

import android.support.test.InstrumentationRegistry
import android.support.test.filters.LargeTest
import android.support.test.runner.AndroidJUnit4
import android.util.Log
import com.pitstop.TripTestUtil
import com.pitstop.models.trip.CarActivity
import junit.framework.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Created by Karol Zdebel on 6/5/2018.
 */

@RunWith(AndroidJUnit4::class)
@LargeTest
class LocalActivityStorageTest {

    private val tag = LocalLocationStorageTest::class.java.simpleName
    private val VIN = "1GB0CVCL7BF147611"
    private lateinit var localActivityStorage: LocalActivityStorage

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getTargetContext()
        localActivityStorage = LocalActivityStorage(context)
    }

    @Test
    fun localActivityStorageTest(){
        Log.d(tag,"running localActivityStorageTest()")

        localActivityStorage.removeAll()

        val carActivity = arrayListOf<CarActivity>()
        for (i in 1..10){
            carActivity.add(TripTestUtil.getRandomCarActivity(VIN,i))
        }

        Assert.assertEquals(10,localActivityStorage.store(carActivity))
        Assert.assertEquals(carActivity,localActivityStorage.getAll())
        localActivityStorage.remove(carActivity[0])
        carActivity.removeAt(0)
        Assert.assertEquals(carActivity,localActivityStorage.getAll())
        localActivityStorage.remove(carActivity)
        Assert.assertTrue(localActivityStorage.getAll().isEmpty())

        localActivityStorage.removeAll()
    }
}