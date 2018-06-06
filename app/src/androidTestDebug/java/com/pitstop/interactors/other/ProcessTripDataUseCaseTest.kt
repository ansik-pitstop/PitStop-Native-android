package com.pitstop.interactors.other

import android.support.test.InstrumentationRegistry
import android.support.test.filters.LargeTest
import android.support.test.runner.AndroidJUnit4
import android.util.Log
import com.google.android.gms.location.DetectedActivity
import com.pitstop.TripTestUtil
import com.pitstop.database.LocalActivityStorage
import com.pitstop.database.LocalLocationStorage
import com.pitstop.dependency.ContextModule
import com.pitstop.dependency.DaggerUseCaseComponent
import com.pitstop.dependency.UseCaseComponent
import com.pitstop.models.trip.CarActivity
import com.pitstop.models.trip.CarLocation
import junit.framework.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Created by Karol Zdebel on 6/5/2018.
 */

@RunWith(AndroidJUnit4::class)
@LargeTest
class ProcessTripDataUseCaseTest {

    private val TAG = ProcessTripDataUseCaseTest::class.java.simpleName

    private val VIN = "1GB0CVCL7BF147611"

    private lateinit var useCaseComponent: UseCaseComponent
    private lateinit var localLocationStorage: LocalLocationStorage
    private lateinit var localActivityStorage: LocalActivityStorage

    @Before
    fun setup() {
        Log.i(TAG, "running setup()")
        val context = InstrumentationRegistry.getTargetContext()
        useCaseComponent = DaggerUseCaseComponent.builder()
                .contextModule(ContextModule(context))
                .build()
        localLocationStorage = LocalLocationStorage(context)
        localLocationStorage.removeAll()
        localActivityStorage = LocalActivityStorage(context)
        localActivityStorage.removeAll()
    }

    @Test
    fun emptyTest(){
        Log.d(TAG, "emptyTest()")
        val completableFuture = CompletableFuture<List<List<CarLocation>>>()
        val expected = arrayListOf<List<CarLocation>>()

        localLocationStorage.removeAll()
        localActivityStorage.removeAll()

        useCaseComponent.processTripDataUseCase().execute(object: ProcessTripDataUseCase.Callback{
            override fun processed(trip: List<List<CarLocation>>) {
                Log.d(TAG,"processed trip: $trip")
                completableFuture.complete(trip)
            }

        })

        try {
            val result = completableFuture.get(10000, TimeUnit.MILLISECONDS)
            Assert.assertEquals(expected, result)
            localLocationStorage.removeAll()
            localActivityStorage.removeAll()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        } catch (e: ExecutionException) {
            e.printStackTrace()
        } catch (e: TimeoutException) {
            e.printStackTrace()
        }

    }

    @Test
    fun hardStartHardEndTest(){
        Log.d(TAG,"hardStartHardEndTest()")
        val completableFuture = CompletableFuture<List<List<CarLocation>>>()

        localLocationStorage.removeAll()
        localActivityStorage.removeAll()

        val carLocationList = arrayListOf<CarLocation>()
        val carActivityList = arrayListOf<CarActivity>()

        carActivityList.add(getLowConfidenceCarActivity(0))
        carActivityList.add(getLowConfidenceFootActivity(0))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,0))
        carActivityList.add(getLowConfidenceCarActivity(1))
        carActivityList.add(getHighConfidenceFootActivity(1))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,1))

        //Start
        carActivityList.add(getHighConfidenceCarActivity(2))
        carActivityList.add(getLowConfidenceFootActivity(2))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,2))
        carActivityList.add(getLowConfidenceFootActivity(3))
        carActivityList.add(getLowConfidenceCarActivity(3))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,3))
        carActivityList.add(getHighConfidenceFootActivity(4))
        carActivityList.add(getLowConfidenceCarActivity(4))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,4))

        //End
        carActivityList.add(getLowConfidenceCarActivity(5))
        carActivityList.add(getLowConfidenceFootActivity(5))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,5))

        localLocationStorage.store(carLocationList)
        localActivityStorage.store(carActivityList)

        useCaseComponent.processTripDataUseCase().execute(object: ProcessTripDataUseCase.Callback{
            override fun processed(trip: List<List<CarLocation>>) {
                Log.d(TAG,"processed trip: $trip")
                completableFuture.complete(trip)
            }

        })

        try {
            val result = completableFuture.get(10000, TimeUnit.MILLISECONDS)
            val expected = carLocationList.subList(2,5)
            Assert.assertTrue(result.size == 1)
            Assert.assertEquals(expected, result[0])
            localLocationStorage.removeAll()
            localActivityStorage.removeAll()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        } catch (e: ExecutionException) {
            e.printStackTrace()
        } catch (e: TimeoutException) {
            e.printStackTrace()
        }

    }

    @Test
    fun softStartHardStartHardEndTest(){
        Log.d(TAG,"hardStartHardEndTest()")
        val completableFuture = CompletableFuture<List<List<CarLocation>>>()

        localLocationStorage.removeAll()
        localActivityStorage.removeAll()

        val carLocationList = arrayListOf<CarLocation>()
        val carActivityList = arrayListOf<CarActivity>()

        carActivityList.add(getLowConfidenceCarActivity(0))
        carActivityList.add(getLowConfidenceFootActivity(0))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,0))
        carActivityList.add(getLowConfidenceCarActivity(1))
        carActivityList.add(getHighConfidenceFootActivity(1))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,1))

        //Soft Start
        carActivityList.add(getSoftConfidenceCarActivity(2))
        carActivityList.add(getLowConfidenceFootActivity(2))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,2))
        carActivityList.add(getLowConfidenceFootActivity(3))
        carActivityList.add(getLowConfidenceCarActivity(3))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,3))

        //Hard Start
        carActivityList.add(getHighConfidenceCarActivity(4))
        carActivityList.add(getLowConfidenceCarActivity(4))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,4))
        carActivityList.add(getLowConfidenceCarActivity(5))
        carActivityList.add(getLowConfidenceFootActivity(5))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,5))
        carActivityList.add(getHighConfidenceFootActivity(6))
        carActivityList.add(getLowConfidenceCarActivity(6))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,6))

        //End
        carActivityList.add(getLowConfidenceFootActivity(7))
        carActivityList.add(getLowConfidenceCarActivity(7))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,7))
        carActivityList.add(getHighConfidenceCarActivity(8))
        carActivityList.add(getLowConfidenceFootActivity(8))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,8))


        localLocationStorage.store(carLocationList)
        localActivityStorage.store(carActivityList)

        useCaseComponent.processTripDataUseCase().execute(object: ProcessTripDataUseCase.Callback{
            override fun processed(trip: List<List<CarLocation>>) {
                Log.d(TAG,"processed trip: $trip")
                completableFuture.complete(trip)
            }

        })

        try {
            val result = completableFuture.get(10000, TimeUnit.MILLISECONDS)
            val expected = carLocationList.subList(2,7)
            Assert.assertTrue(result.size == 1)
            Assert.assertEquals(expected, result[0])
            localLocationStorage.removeAll()
            localActivityStorage.removeAll()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        } catch (e: ExecutionException) {
            e.printStackTrace()
        } catch (e: TimeoutException) {
            e.printStackTrace()
        }

    }

    @Test
    fun softStartHardStartSoftEndNoFinishTest(){
        Log.d(TAG, "softStartHardStartSoftEndNoFinishTest() started")

        val completableFuture = CompletableFuture<List<List<CarLocation>>>()

        localLocationStorage.removeAll()
        localActivityStorage.removeAll()

        val carLocationList = arrayListOf<CarLocation>()
        val carActivityList = arrayListOf<CarActivity>()

        carActivityList.add(getLowConfidenceCarActivity(0))
        carActivityList.add(getLowConfidenceFootActivity(0))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,0))
        carActivityList.add(getLowConfidenceCarActivity(1))
        carActivityList.add(getHighConfidenceFootActivity(1))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,1))

        //Soft Start
        carActivityList.add(getSoftConfidenceCarActivity(2))
        carActivityList.add(getLowConfidenceFootActivity(2))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,2))
        carActivityList.add(getLowConfidenceFootActivity(3))
        carActivityList.add(getLowConfidenceCarActivity(3))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,3))

        //Hard Start
        carActivityList.add(getHighConfidenceCarActivity(4))
        carActivityList.add(getLowConfidenceCarActivity(4))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,4))
        carActivityList.add(getLowConfidenceCarActivity(5))
        carActivityList.add(getLowConfidenceFootActivity(5))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,5))
        carActivityList.add(getHighConfidenceStillActivity(6))
        carActivityList.add(getLowConfidenceCarActivity(6))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,6))

        //Soft End
        carActivityList.add(getLowConfidenceFootActivity(7))
        carActivityList.add(getLowConfidenceCarActivity(7))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,7))
        carActivityList.add(getHighConfidenceCarActivity(8))
        carActivityList.add(getLowConfidenceFootActivity(8))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,8))

        //No Finish

        localLocationStorage.store(carLocationList)
        localActivityStorage.store(carActivityList)

        useCaseComponent.processTripDataUseCase().execute(object: ProcessTripDataUseCase.Callback{
            override fun processed(trip: List<List<CarLocation>>) {
                Log.d(TAG,"processed trip: $trip")
                completableFuture.complete(trip)
            }

        })

        try {
            val result = completableFuture.get(10000, TimeUnit.MILLISECONDS)
            val expected = arrayListOf<CarLocation>()
            Assert.assertTrue(result.isEmpty())
            localLocationStorage.removeAll()
            localActivityStorage.removeAll()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        } catch (e: ExecutionException) {
            e.printStackTrace()
        } catch (e: TimeoutException) {
            e.printStackTrace()
        }

    }

    @Test
    fun softStartHardStartSoftEndFinishTest(){
        Log.d(TAG, "softStartHardStartSoftEndNoFinishTest() started")

        val completableFuture = CompletableFuture<List<List<CarLocation>>>()

        localLocationStorage.removeAll()
        localActivityStorage.removeAll()

        val carLocationList = arrayListOf<CarLocation>()
        val carActivityList = arrayListOf<CarActivity>()

        carActivityList.add(getLowConfidenceCarActivity(0))
        carActivityList.add(getLowConfidenceFootActivity(0))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,0))
        carActivityList.add(getLowConfidenceCarActivity(1))
        carActivityList.add(getHighConfidenceFootActivity(1))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,1))

        //Soft Start
        carActivityList.add(getSoftConfidenceCarActivity(2))
        carActivityList.add(getLowConfidenceFootActivity(2))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,2))
        carActivityList.add(getLowConfidenceFootActivity(3))
        carActivityList.add(getLowConfidenceCarActivity(3))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,3))

        //Hard Start
        carActivityList.add(getHighConfidenceCarActivity(4))
        carActivityList.add(getLowConfidenceFootActivity(4))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,4))
        carActivityList.add(getLowConfidenceCarActivity(5))
        carActivityList.add(getLowConfidenceFootActivity(5))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,5))
        carActivityList.add(getHighConfidenceStillActivity(6))
        carActivityList.add(getLowConfidenceCarActivity(6))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,6))

        //Soft End
        carActivityList.add(getLowConfidenceFootActivity(701))
        carActivityList.add(getLowConfidenceCarActivity(701))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,701))
        carActivityList.add(getHighConfidenceCarActivity(702))
        carActivityList.add(getLowConfidenceFootActivity(702))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,702))

        //No Finish

        localLocationStorage.store(carLocationList)
        localActivityStorage.store(carActivityList)

        useCaseComponent.processTripDataUseCase().execute(object: ProcessTripDataUseCase.Callback{
            override fun processed(trip: List<List<CarLocation>>) {
                Log.d(TAG,"processed trip: $trip")
                completableFuture.complete(trip)
            }

        })

        try {
            val result = completableFuture.get(10000, TimeUnit.MILLISECONDS)
            val expected = carLocationList.subList(2,7)
            Log.d(TAG,"result: $result")
            Assert.assertTrue(result.size == 1)
            Assert.assertEquals(expected, result[0])
            localLocationStorage.removeAll()
            localActivityStorage.removeAll()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        } catch (e: ExecutionException) {
            e.printStackTrace()
        } catch (e: TimeoutException) {
            e.printStackTrace()
        }

    }

    @Test
    fun softStartAndNothingTest(){
        Log.d(TAG,"softStartAndNothingTest() started")
        val completableFuture = CompletableFuture<List<List<CarLocation>>>()

        localLocationStorage.removeAll()
        localActivityStorage.removeAll()

        val carLocationList = arrayListOf<CarLocation>()
        val carActivityList = arrayListOf<CarActivity>()

        carActivityList.add(getLowConfidenceCarActivity(0))
        carActivityList.add(getHighConfidenceFootActivity(0))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,0))
        carActivityList.add(getHighConfidenceStillActivity(1))
        carActivityList.add(getHighConfidenceFootActivity(1))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,1))

        //Soft Start
        carActivityList.add(getSoftConfidenceCarActivity(2))
        carActivityList.add(getLowConfidenceFootActivity(2))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,2))
        carActivityList.add(getLowConfidenceFootActivity(3))
        carActivityList.add(getLowConfidenceCarActivity(3))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,3))

        //Random Data, no hard start or other alert
        carActivityList.add(getLowConfidenceStillActivity(4))
        carActivityList.add(getLowConfidenceCarActivity(4))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,4))
        carActivityList.add(getLowConfidenceCarActivity(5))
        carActivityList.add(getLowConfidenceFootActivity(5))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,5))
        carActivityList.add(getLowConfidenceStillActivity(6))
        carActivityList.add(getLowConfidenceCarActivity(6))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,6))

        localLocationStorage.store(carLocationList)
        localActivityStorage.store(carActivityList)

        useCaseComponent.processTripDataUseCase().execute(object: ProcessTripDataUseCase.Callback{
            override fun processed(trip: List<List<CarLocation>>) {
                Log.d(TAG,"processed trip: $trip")
                completableFuture.complete(trip)
            }

        })

        try {
            val result = completableFuture.get(10000, TimeUnit.MILLISECONDS)
            Assert.assertTrue(result.isEmpty())
            localLocationStorage.removeAll()
            localActivityStorage.removeAll()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        } catch (e: ExecutionException) {
            e.printStackTrace()
        } catch (e: TimeoutException) {
            e.printStackTrace()
        }

    }

    @Test
    fun hardStartAndNothingTest(){
        Log.d(TAG,"hardStartAndNothingTest() started")
        val completableFuture = CompletableFuture<List<List<CarLocation>>>()

        localLocationStorage.removeAll()
        localActivityStorage.removeAll()

        val carLocationList = arrayListOf<CarLocation>()
        val carActivityList = arrayListOf<CarActivity>()

        carActivityList.add(getLowConfidenceCarActivity(0))
        carActivityList.add(getHighConfidenceFootActivity(0))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,0))
        carActivityList.add(getHighConfidenceStillActivity(1))
        carActivityList.add(getHighConfidenceFootActivity(1))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,1))

        //Hard Start
        carActivityList.add(getHighConfidenceCarActivity(2))
        carActivityList.add(getLowConfidenceFootActivity(2))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,2))
        carActivityList.add(getLowConfidenceFootActivity(3))
        carActivityList.add(getLowConfidenceCarActivity(3))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,3))

        //Random Data, no hard start or other alert
        carActivityList.add(getLowConfidenceStillActivity(4))
        carActivityList.add(getLowConfidenceCarActivity(4))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,4))
        carActivityList.add(getLowConfidenceCarActivity(5))
        carActivityList.add(getLowConfidenceFootActivity(5))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,5))
        carActivityList.add(getLowConfidenceStillActivity(6))
        carActivityList.add(getLowConfidenceCarActivity(6))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,6))

        localLocationStorage.store(carLocationList)
        localActivityStorage.store(carActivityList)

        useCaseComponent.processTripDataUseCase().execute(object: ProcessTripDataUseCase.Callback{
            override fun processed(trip: List<List<CarLocation>>) {
                Log.d(TAG,"processed trip: $trip")
                completableFuture.complete(trip)
            }

        })

        try {
            val result = completableFuture.get(10000, TimeUnit.MILLISECONDS)
            Assert.assertTrue(result.isEmpty())
            localLocationStorage.removeAll()
            localActivityStorage.removeAll()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        } catch (e: ExecutionException) {
            e.printStackTrace()
        } catch (e: TimeoutException) {
            e.printStackTrace()
        }

    }

    @Test
    fun softStartAndHardEndTest(){
        Log.d(TAG,"softStartAndHardEndTest() started")
        val completableFuture = CompletableFuture<List<List<CarLocation>>>()

        localLocationStorage.removeAll()
        localActivityStorage.removeAll()

        val carLocationList = arrayListOf<CarLocation>()
        val carActivityList = arrayListOf<CarActivity>()

        carActivityList.add(getLowConfidenceCarActivity(0))
        carActivityList.add(getHighConfidenceFootActivity(0))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,0))
        carActivityList.add(getHighConfidenceStillActivity(1))
        carActivityList.add(getHighConfidenceFootActivity(1))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,1))

        //Soft Start
        carActivityList.add(getSoftConfidenceCarActivity(2))
        carActivityList.add(getLowConfidenceFootActivity(2))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,2))
        carActivityList.add(getLowConfidenceFootActivity(3))
        carActivityList.add(getLowConfidenceCarActivity(3))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,3))
        carActivityList.add(getHighConfidenceFootActivity(4))
        carActivityList.add(getLowConfidenceCarActivity(4))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,4))

        //Hard End
        carActivityList.add(getLowConfidenceCarActivity(5))
        carActivityList.add(getLowConfidenceFootActivity(5))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,5))
        carActivityList.add(getLowConfidenceStillActivity(6))
        carActivityList.add(getLowConfidenceCarActivity(6))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,6))

        localLocationStorage.store(carLocationList)
        localActivityStorage.store(carActivityList)

        useCaseComponent.processTripDataUseCase().execute(object: ProcessTripDataUseCase.Callback{
            override fun processed(trip: List<List<CarLocation>>) {
                Log.d(TAG,"processed trip: $trip")
                completableFuture.complete(trip)
            }

        })

        try {
            val result = completableFuture.get(10000, TimeUnit.MILLISECONDS)
            Assert.assertTrue(result.isEmpty())
            localLocationStorage.removeAll()
            localActivityStorage.removeAll()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        } catch (e: ExecutionException) {
            e.printStackTrace()
        } catch (e: TimeoutException) {
            e.printStackTrace()
        }

    }

    fun getHighConfidenceCarActivity(indexTimeOffset: Int): CarActivity
            = CarActivity(VIN,(1000*indexTimeOffset).toLong()
                   ,DetectedActivity.IN_VEHICLE,75)

    fun getLowConfidenceCarActivity(indexTimeOffset: Int): CarActivity
            = CarActivity(VIN,(1000*indexTimeOffset).toLong()
            ,DetectedActivity.IN_VEHICLE,25)

    fun getHighConfidenceFootActivity(indexTimeOffset: Int): CarActivity
            = CarActivity(VIN,(1000*indexTimeOffset).toLong()
            ,DetectedActivity.ON_FOOT,95)

    fun getLowConfidenceFootActivity(indexTimeOffset: Int): CarActivity
            = CarActivity(VIN,(1000*indexTimeOffset).toLong()
            ,DetectedActivity.ON_FOOT,10)

    fun getHighConfidenceStillActivity(indexTimeOffset: Int): CarActivity
            = CarActivity(VIN,(1000*indexTimeOffset).toLong()
            ,DetectedActivity.STILL,100)

    fun getLowConfidenceStillActivity(indexTimeOffset: Int): CarActivity
            = CarActivity(VIN,(1000*indexTimeOffset).toLong()
            ,DetectedActivity.STILL,15)

    fun getSoftConfidenceCarActivity(indexTimeOffset: Int): CarActivity
            = CarActivity(VIN,(1000*indexTimeOffset).toLong()
            , DetectedActivity.IN_VEHICLE, 35)
}