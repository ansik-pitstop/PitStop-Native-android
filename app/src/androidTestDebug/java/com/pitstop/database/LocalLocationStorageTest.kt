package com.pitstop.database

import android.support.test.InstrumentationRegistry
import android.support.test.filters.LargeTest
import android.support.test.runner.AndroidJUnit4
import android.util.Log
import com.pitstop.TripTestUtil
import com.pitstop.models.trip.CarLocation
import junit.framework.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Created by Karol Zdebel on 6/5/2018.
 */

@RunWith(AndroidJUnit4::class)
@LargeTest
class LocalLocationStorageTest {

    private val tag = LocalLocationStorageTest::class.java.simpleName
    private val VIN = "1GB0CVCL7BF147611"
    private lateinit var localLocationStorage: LocalLocationStorage

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getTargetContext()
        localLocationStorage = LocalLocationStorage(LocalDatabaseHelper.getInstance(context))
    }

    @Test
    fun localLocationStorageTest(){
        Log.d(tag,"running localLocationStorageTest()")

        localLocationStorage.removeAll()

        val carLocations = arrayListOf<CarLocation>()
        for (i in 1..10){
            carLocations.add(TripTestUtil.getRandomCarLocation(VIN,i))
        }

        Assert.assertEquals(10,localLocationStorage.store(carLocations))
        Assert.assertEquals(carLocations,localLocationStorage.getAll())
        localLocationStorage.remove(carLocations[0])
        carLocations.removeAt(0)
        Assert.assertEquals(carLocations,localLocationStorage.getAll())
        localLocationStorage.remove(carLocations)
        Assert.assertTrue(localLocationStorage.getAll().isEmpty())

        localLocationStorage.removeAll()
    }
}