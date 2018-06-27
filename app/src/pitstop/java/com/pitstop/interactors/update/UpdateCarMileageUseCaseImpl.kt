package com.pitstop.interactors.update

import android.os.Handler
import android.util.Log
import com.pitstop.models.DebugMessage
import com.pitstop.models.Settings
import com.pitstop.network.RequestError
import com.pitstop.repositories.CarRepository
import com.pitstop.repositories.Repository
import com.pitstop.repositories.UserRepository
import com.pitstop.utils.Logger
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

/**
 * Created by Karol Zdebel on 9/7/2017.
 */

class UpdateCarMileageUseCaseImpl(private val carRepository: CarRepository, private val userRepository: UserRepository, private val usecaseHandler: Handler, private val mainHandler: Handler) : UpdateCarMileageUseCase {

    private val TAG = javaClass.simpleName
    private val compositeDisposable = CompositeDisposable()

    private var callback: UpdateCarMileageUseCase.Callback? = null
    private var mileage: Double = 0.toDouble()

    override fun execute(mileage: Double, callback: UpdateCarMileageUseCase.Callback) {
        Logger.getInstance()!!.logI(TAG, "Use case execution started: mileage=$mileage", DebugMessage.TYPE_USE_CASE)
        this.callback = callback
        this.mileage = mileage
        usecaseHandler.post(this)
    }

    private fun onMileageUpdated() {
        Logger.getInstance()!!.logI(TAG, "Use case finished: mileage updated", DebugMessage.TYPE_USE_CASE)
        compositeDisposable.clear()
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
        userRepository.getCurrentUserSettings(object : Repository.Callback<Settings> {
            override fun onSuccess(settings: Settings) {
                Log.d(TAG, "got current user settings: $settings")
                if (!settings.hasMainCar()) {
                    this@UpdateCarMileageUseCaseImpl.onNoCarAdded()
                    return
                }

                val disposable = carRepository.updateMileage(settings.carId, mileage)
                        .subscribeOn(Schedulers.computation())
                        .observeOn(Schedulers.io())
                        .subscribe({ next -> this@UpdateCarMileageUseCaseImpl.onMileageUpdated()
                        }) { err ->
                            this@UpdateCarMileageUseCaseImpl.onError(RequestError(err))
                        }
                compositeDisposable.add(disposable)

            }

            override fun onError(error: RequestError) {
                this@UpdateCarMileageUseCaseImpl.onError(error)
            }
        })
    }
}
