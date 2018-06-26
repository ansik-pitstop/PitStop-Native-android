package com.pitstop.interactors.other

import android.support.test.InstrumentationRegistry
import android.support.test.filters.LargeTest
import android.support.test.runner.AndroidJUnit4
import android.util.Log
import com.pitstop.dependency.ContextModule
import com.pitstop.dependency.DaggerUseCaseComponent
import com.pitstop.dependency.UseCaseComponent
import com.pitstop.network.RequestError
import junit.framework.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Created by Karol Zdebel on 6/26/2018.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class HandleVinOnConnectUseCaseTest {


    private val TAG = javaClass.simpleName
    private lateinit var useCaseComponent: UseCaseComponent

    @Before
    fun setup() {
        Log.i(TAG, "running setup()")
        val context = InstrumentationRegistry.getTargetContext()
        useCaseComponent = DaggerUseCaseComponent.builder()
                .contextModule(ContextModule(context))
                .build()
    }

    @Test
    fun handleVinOnConnectUseCaseTest() {
        val deviceId = ""
        val vin = "1GCEC14H6CS190284"

        val completableFuture = CompletableFuture<Boolean>()

        useCaseComponent.handleVinOnConnectUseCase().execute(vin,deviceId,object: HandleVinOnConnectUseCase.Callback{
            override fun onSuccess() {
                Log.d(TAG,"onSuccess()")
                completableFuture.complete(true)
            }

            override fun onDeviceBrokenAndCarMissingScanner() {
                Log.d(TAG,"onDeviceBrokenAndCarMissingScanner()")
                completableFuture.complete(false)
            }

            override fun onDeviceBrokenAndCarHasScanner(scannerId: String?) {
                Log.d(TAG,"onDeviceBrokenAndCarHasScanner()")
                completableFuture.complete(false)
            }

            override fun onDeviceInvalid() {
                Log.d(TAG,"onDeviceInvalid()")
                completableFuture.complete(false)
            }

            override fun onDeviceAlreadyActive() {
                Log.d(TAG,"onDeviceAlreadyActive()")
                completableFuture.complete(false)
            }

            override fun onError(error: RequestError?) {
                Log.d(TAG,"onError()")
                completableFuture.complete(false)
            }
        })

        try {
            val result = completableFuture.get(10000, TimeUnit.MILLISECONDS)
            assertTrue(result)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        } catch (e: ExecutionException) {
            e.printStackTrace()
        } catch (e: TimeoutException) {
            e.printStackTrace()
        }

    }
}