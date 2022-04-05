package com.pitstop.interactors.update

import android.os.Handler
import android.util.Log
import com.pitstop.EventBus.CarDataChangedEvent
import com.pitstop.EventBus.EventSource
import com.pitstop.EventBus.EventType
import com.pitstop.EventBus.EventTypeImpl
import com.pitstop.models.DebugMessage
import com.pitstop.models.Settings
import com.pitstop.network.RequestError
import com.pitstop.repositories.CarRepository
import com.pitstop.repositories.Repository
import com.pitstop.repositories.UserRepository
import com.pitstop.utils.Logger
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus

/**
 * Created by Karol Zdebel on 9/7/2017.
 */

class UpdateCarMileageUseCaseImpl(private val carRepository: CarRepository
                                  , private val userRepository: UserRepository
                                  , private val usecaseHandler: Handler
                                  , private val mainHandler: Handler) : UpdateCarMileageUseCase {

    private val TAG = javaClass.simpleName
    private val compositeDisposable = CompositeDisposable()

    private var callback: UpdateCarMileageUseCase.Callback? = null
    private var mileage: Double = 0.toDouble()
    private lateinit var eventSource: EventSource
    private var carId: Int = 0

    override fun execute(carId: Int, mileage: Double, eventSource: EventSource, callback: UpdateCarMileageUseCase.Callback) {
        Logger.getInstance()!!.logI(TAG, "Use case execution started: mileage=$mileage", DebugMessage.TYPE_USE_CASE)
        this.callback = callback
        this.mileage = mileage
        this.eventSource = eventSource
        this.carId = carId
        usecaseHandler.post(this)
    }

    private fun onMileageUpdated() {
        Logger.getInstance()!!.logI(TAG, "Use case finished: mileage updated", DebugMessage.TYPE_USE_CASE)
        compositeDisposable.clear()
        val eventType = EventTypeImpl(EventType.EVENT_MILEAGE)
        EventBus.getDefault().post(CarDataChangedEvent(eventType, eventSource))
        mainHandler.post { callback!!.onMileageUpdated() }
    }

    private fun onNoCarAdded() {
        Logger.getInstance()!!.logI(TAG, "Use case finished: no car added", DebugMessage.TYPE_USE_CASE)
        compositeDisposable.clear()
        mainHandler.post { callback!!.onNoCarAdded() }
    }

    private fun onError(error: RequestError) {
        Logger.getInstance()!!.logI(TAG, "Use case returned error: err=$error", DebugMessage.TYPE_USE_CASE)
        compositeDisposable.clear()
        mainHandler.post { callback!!.onError(error) }
    }

    override fun run() {
        Log.d(TAG, "run()")
        val disposable = carRepository.updateMileage(this.carId, mileage)
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.io())
                .subscribe({ next -> this@UpdateCarMileageUseCaseImpl.onMileageUpdated()
                }) { err ->
                    this@UpdateCarMileageUseCaseImpl.onError(RequestError(err))
                }
        compositeDisposable.add(disposable)
    }
}
