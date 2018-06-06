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

    @Test
    fun softStartSoftEndFinishTest(){
        Log.d(TAG, "softStartSoftEndFinishTest() started")

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

        //Filler Data
        carActivityList.add(getLowConfidenceStillActivity(4))
        carActivityList.add(getLowConfidenceFootActivity(4))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,4))
        carActivityList.add(getLowConfidenceCarActivity(5))
        carActivityList.add(getLowConfidenceFootActivity(5))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,5))
        carActivityList.add(getHighConfidenceStillActivity(6))
        carActivityList.add(getLowConfidenceCarActivity(6))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,6))

        //Soft End
        carActivityList.add(getLowConfidenceFootActivity(900))
        carActivityList.add(getLowConfidenceCarActivity(900))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,900))
        carActivityList.add(getHighConfidenceCarActivity(905))
        carActivityList.add(getLowConfidenceFootActivity(905))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,905))

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
    fun softStartHardStartAndNothingTest(){
        Log.d(TAG,"softStartHardStartAndNothingTest() started")
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

        //Filler data, but no alerts
        carActivityList.add(getLowConfidenceCarActivity(5))
        carActivityList.add(getLowConfidenceFootActivity(5))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,5))
        carActivityList.add(getLowConfidenceStillActivity(6))
        carActivityList.add(getLowConfidenceCarActivity(6))
        carActivityList.add(getLowConfidenceFootActivity(6))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,6))
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

    //Two trips recorded back to back with still period in between
    @Test
    fun twoBasicTripTest(){
        Log.d(TAG,"twoBasicTripTest() started")
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

        //Filler data, but no alerts
        carActivityList.add(getLowConfidenceCarActivity(5))
        carActivityList.add(getLowConfidenceFootActivity(5))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,5))
        carActivityList.add(getLowConfidenceStillActivity(6))
        carActivityList.add(getLowConfidenceCarActivity(6))
        carActivityList.add(getLowConfidenceFootActivity(6))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,6))

        //Hard End 7
        carActivityList.add(getHighConfidenceFootActivity(500))
        carActivityList.add(getLowConfidenceCarActivity(500))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,500))

        //Still activity period 8
        carActivityList.add(getHighConfidenceStillActivity(600))
        carActivityList.add(getLowConfidenceFootActivity(600))
        carActivityList.add(getLowConfidenceCarActivity(600))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,600))

        //Hard Start 9
        carActivityList.add(getHighConfidenceCarActivity(1200))
        carActivityList.add(getLowConfidenceFootActivity(1200))
        carActivityList.add(getLowConfidenceStillActivity(1200))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,1200))

        //Filler Data 10 & 11
        carActivityList.add(getLowConfidenceCarActivity(1500))
        carActivityList.add(getLowConfidenceFootActivity(1500))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,1500))
        carActivityList.add(getLowConfidenceStillActivity(1501))
        carActivityList.add(getLowConfidenceCarActivity(1501))
        carActivityList.add(getLowConfidenceFootActivity(1501))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,1501))

        //Soft End Start 12
        carActivityList.add(getHighConfidenceStillActivity(1600))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,1600))

        //Some data between Soft end start and end 13
        carActivityList.add(getLowConfidenceCarActivity(1800))
        carActivityList.add(getLowConfidenceFootActivity(1800))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,1800))

        //Soft End Finish 14
        carActivityList.add(getLowConfidenceFootActivity(2201))
        carActivityList.add(getLowConfidenceCarActivity(2201))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,2201))

        //Random location point 15
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,2300))


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
            Assert.assertTrue(result.size == 2)
            Log.d(TAG,"result[1]: ${result[1]}")
            Log.d(TAG,"expected: ${carLocationList.subList(9,13)}")
            Assert.assertEquals(carLocationList.subList(2,8),result[0])
            Assert.assertEquals(carLocationList.subList(9,13),result[1])
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

    //Test is here primarily to check whether soft end variable will be reset by another soft start
    @Test
    fun softStartHardStartSoftEndStartSoftStartSoftEndStartAndFinishTest(){
        Log.d(TAG, "softStartHardStartSoftEndStartSoftStartSoftEndStartAndFinishTest() started")

        val completableFuture = CompletableFuture<List<List<CarLocation>>>()

        localLocationStorage.removeAll()
        localActivityStorage.removeAll()

        val carLocationList = arrayListOf<CarLocation>()
        val carActivityList = arrayListOf<CarActivity>()

        //0 & 1
        carActivityList.add(getLowConfidenceCarActivity(0))
        carActivityList.add(getLowConfidenceFootActivity(0))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,0))
        carActivityList.add(getLowConfidenceCarActivity(1))
        carActivityList.add(getHighConfidenceFootActivity(1))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,1))

        //Soft Start 2 & 3
        carActivityList.add(getSoftConfidenceCarActivity(2))
        carActivityList.add(getLowConfidenceFootActivity(2))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,2))
        carActivityList.add(getLowConfidenceFootActivity(3))
        carActivityList.add(getLowConfidenceCarActivity(3))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,3))

        //Hard Start 4 - 6
        carActivityList.add(getHighConfidenceCarActivity(4))
        carActivityList.add(getLowConfidenceCarActivity(4))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,4))
        carActivityList.add(getLowConfidenceCarActivity(5))
        carActivityList.add(getLowConfidenceFootActivity(5))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,5))
        carActivityList.add(getLowConfidenceStillActivity(6))
        carActivityList.add(getLowConfidenceCarActivity(6))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,6))

        //Soft End 7-8
        carActivityList.add(getHighConfidenceStillActivity(7))
        carActivityList.add(getLowConfidenceCarActivity(7))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,7))
        carActivityList.add(getLowConfidenceCarActivity(8))
        carActivityList.add(getLowConfidenceFootActivity(8))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,8))

        //Soft Start 9
        carActivityList.add(getSoftConfidenceCarActivity(200))
        carActivityList.add(getLowConfidenceFootActivity(200))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,200))

        //Filler Data 10
        carActivityList.add(getLowConfidenceCarActivity(901))
        carActivityList.add(getLowConfidenceFootActivity(901))
        carActivityList.add(getLowConfidenceStillActivity(901))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,901))

        //Soft End Start 11
        carActivityList.add(getHighConfidenceStillActivity(1501))
        carActivityList.add(getLowConfidenceFootActivity(1501))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,1501))


        //Filler Data while still 12
        carActivityList.add(getLowConfidenceCarActivity(1801))
        carActivityList.add(getLowConfidenceCarActivity(1801))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,1801))

        //Soft End Finish 13
        carActivityList.add(getLowConfidenceCarActivity(2102))
        carActivityList.add(getLowConfidenceFootActivity(2102))
        carActivityList.add(getLowConfidenceStillActivity(2102))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,2102))

        //Random location points 14
        carActivityList.add(getHighConfidenceCarActivity(2502))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,2502))


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
            val expected = carLocationList.subList(2,12)
            Assert.assertTrue(result.size == 1)
            Assert.assertEquals(expected,result[0])
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

    //Two trips recorded back to back then trip with no end
    @Test
    fun twoTripsThenTripWithNothingTest(){
        Log.d(TAG,"twoTripsThenTripWithNothingTest() started")
        val completableFuture = CompletableFuture<List<List<CarLocation>>>()

        localLocationStorage.removeAll()
        localActivityStorage.removeAll()

        val carLocationList = arrayListOf<CarLocation>()
        val carActivityList = arrayListOf<CarActivity>()

        // 0 - 1
        carActivityList.add(getLowConfidenceCarActivity(0))
        carActivityList.add(getLowConfidenceFootActivity(0))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,0))
        carActivityList.add(getLowConfidenceCarActivity(1))
        carActivityList.add(getHighConfidenceFootActivity(1))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,1))

        //Soft Start 2-3
        carActivityList.add(getSoftConfidenceCarActivity(2))
        carActivityList.add(getLowConfidenceFootActivity(2))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,2))
        carActivityList.add(getLowConfidenceFootActivity(3))
        carActivityList.add(getLowConfidenceCarActivity(3))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,3))

        //Filler data, but no alerts 4-5
        carActivityList.add(getLowConfidenceCarActivity(5))
        carActivityList.add(getLowConfidenceFootActivity(5))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,5))
        carActivityList.add(getLowConfidenceStillActivity(6))
        carActivityList.add(getLowConfidenceCarActivity(6))
        carActivityList.add(getLowConfidenceFootActivity(6))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,6))

        //Soft End Start 6
        carActivityList.add(getHighConfidenceFootActivity(500))
        carActivityList.add(getLowConfidenceCarActivity(500))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,500))

        //Still activity period 7
        carActivityList.add(getHighConfidenceStillActivity(600))
        carActivityList.add(getLowConfidenceFootActivity(600))
        carActivityList.add(getLowConfidenceCarActivity(600))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,600))

        //Hard Start 8
        carActivityList.add(getHighConfidenceCarActivity(1200))
        carActivityList.add(getLowConfidenceFootActivity(1200))
        carActivityList.add(getLowConfidenceStillActivity(1200))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,1200))

        //Filler Data 9-10
        carActivityList.add(getLowConfidenceCarActivity(3300))
        carActivityList.add(getLowConfidenceFootActivity(3300))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,3300))
        carActivityList.add(getLowConfidenceStillActivity(3301))
        carActivityList.add(getLowConfidenceCarActivity(3301))
        carActivityList.add(getLowConfidenceFootActivity(3301))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,3301))

        //Hard End 11
        carActivityList.add(getHighConfidenceFootActivity(3500))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,3500))

        //Some data after 12
        carActivityList.add(getLowConfidenceCarActivity(3600))
        carActivityList.add(getLowConfidenceFootActivity(3600))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,3600))

        //Soft Start 13
        carActivityList.add(getLowConfidenceFootActivity(3700))
        carActivityList.add(getSoftConfidenceCarActivity(3700))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,3700))


        //Some data 14
        carActivityList.add(getLowConfidenceCarActivity(3705))
        carActivityList.add(getLowConfidenceFootActivity(3705))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,3705))

        //Hard Start 15
        carActivityList.add(getHighConfidenceCarActivity(3800))
        carActivityList.add(getLowConfidenceFootActivity(3800))
        carActivityList.add(getLowConfidenceStillActivity(3800))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,3800))

        //Some data 16
        carActivityList.add(getLowConfidenceCarActivity(3801))
        carActivityList.add(getLowConfidenceFootActivity(3801))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,3801))

        //Hard End 17
        carActivityList.add(getHighConfidenceFootActivity(3802))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,3802))

        //Some data 18
        carActivityList.add(getLowConfidenceCarActivity(3900))
        carActivityList.add(getLowConfidenceFootActivity(3900))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,3900))

        //Hard Start 19
        carActivityList.add(getHighConfidenceCarActivity(3901))
        carActivityList.add(getLowConfidenceFootActivity(3901))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,3901))

        //Filler Data 20
        carActivityList.add(getLowConfidenceCarActivity(4000))
        carActivityList.add(getLowConfidenceFootActivity(4000))
        carActivityList.add(getLowConfidenceFootActivity(4000))
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,4000))

        //Nothing
        carLocationList.add(TripTestUtil.getRandomCarLocation(VIN,4050))

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
            Assert.assertTrue(result.size == 2)
            Log.d(TAG,"result[1]: ${result[1]}")
            Log.d(TAG,"expected: ${carLocationList.subList(9,13)}")
            Assert.assertEquals(carLocationList.subList(2,11),result[0])
            Assert.assertEquals(carLocationList.subList(13,17),result[1])
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