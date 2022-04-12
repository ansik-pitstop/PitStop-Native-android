package com.pitstop.interactors.other

import android.os.Handler
import android.util.Log
import com.pitstop.models.Car
import com.pitstop.models.DebugMessage
import com.pitstop.models.User
import com.pitstop.network.RequestError
import com.pitstop.repositories.CarRepository
import com.pitstop.repositories.Repository
import com.pitstop.repositories.UserRepository
import com.pitstop.retrofit.PitstopSmoochApi
import com.pitstop.utils.Logger
import com.pitstop.utils.SmoochUtil
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.smooch.core.Smooch
import java.io.IOException
import java.util.*

/**
 * Created by Karol Zdebel on 2/26/2018.
 */
class SmoochLoginUseCaseImpl(private val smoochApi: PitstopSmoochApi, private val userRepository: UserRepository
                             , private val carRepository: CarRepository, private val usecaseHandler: Handler
                             , private val mainHandler: Handler): SmoochLoginUseCase {
    private val tag = javaClass.simpleName

    private lateinit var callback: SmoochLoginUseCase.Callback
    private val again = 60000L
    private lateinit var smoochUser: io.smooch.core.User
    private val compositeDisposable: CompositeDisposable = CompositeDisposable()

    override fun execute(smoochUser: io.smooch.core.User, callback: SmoochLoginUseCase.Callback) {
        Logger.getInstance()!!.logI(tag, "Use case execution started"
                , DebugMessage.TYPE_USE_CASE)
        this.callback = callback
        this.smoochUser = smoochUser
        usecaseHandler.post(this)

    }

    override fun run() {
        Log.d(tag,"run()")
        userRepository.getCurrentUser(object: Repository.Callback<User>{
            override fun onSuccess(user: User) {
                Log.d(tag,"got current user: "+user)
                val userId = user.id

                //Set user so that when this use case finishes the user is set and ready for messaging

                try {
                    val call = smoochApi.getSmoochToken(userId).execute()

                    if (!call.isSuccessful) {
                        Log.d(tag, "call unsuccessful")
                        onErrorFound(RequestError.getUnknownError())
                        return
                    }

                    Log.d(tag, "call successful")
                    val body = call.body()

                    if (body == null) {
                        Log.d(tag, "err body null")
                        onErrorFound(RequestError.getUnknownError())
                        return
                    }

                    val smoochToken = body.get("smoochToken").asString
                    Log.d(tag, "smooch token: " + smoochToken)
                    Log.d(tag, "Current thread" + Thread.currentThread())

                    if (smoochToken == null) {
                        Log.d(tag, "err smooch token null")
                        onErrorFound(RequestError.getUnknownError())
                        return
                    }

                    Log.d(tag, "Will login on smooch")
                    Log.d(tag, "userId: $userId")

                    // For some random reason, login on smooch doesn't work anymore when it's
                    // outside a thread, maybe a race condition
//                    loginOnSmooch(user, smoochToken)
                    val task: TimerTask = object : TimerTask() {
                        override fun run() {
                            loginOnSmooch(user, smoochToken)
                        }
                    }
                    val timer = Timer("Timer")
                    val delay = 1000L
                    timer.schedule(task, delay)
                } catch(e: IOException) {
                    onErrorFound(RequestError.getOfflineError())
                }
            }

            override fun onError(error: RequestError) {
                onErrorFound(error)
            }
        })
    }

    fun loginOnSmooch(user: User, smoochToken: String) {
        Smooch.login(user.id.toString(), smoochToken) {
            Log.d(tag, "Logged in on smooch")
            Log.d(tag, "login response err: " + it.error)
            if (it.error == null) {
                //Set user so that when this use case finishes the user is set and ready for messaging
                SmoochUtil.setSmoochProperties(user)
                if (user.settings.hasMainCar()) {
                    val disposable = carRepository.get(user.settings.carId, Repository.DATABASE_TYPE.LOCAL)
                            .subscribeOn(Schedulers.computation())
                            .observeOn(Schedulers.io())
                            .subscribe({next->
                                val car: Car = next.data!!
                                Log.d(tag, next.data.toString())
                                //Set car
                                SmoochUtil.setSmoochProperties(car)
                                Log.d(tag,"set smooch user properties! user: $user")
                                onLogin()
                            }, {error ->
                                Log.e(tag,"error storing custom properties! err: $error")
                                onErrorFound(RequestError.getUnknownError())
                            })
                    compositeDisposable.add(disposable)
                } else {
                    onLogin()
                }
            } else onErrorFound(RequestError.getUnknownError())
        }
    }

    private fun onErrorFound(err: RequestError){
        Logger.getInstance()!!.logE(tag, "Use case returned error: err=$err, trying again in ${again/1000} seconds"
                , DebugMessage.TYPE_USE_CASE)
        usecaseHandler.postDelayed(this,again)
        mainHandler.post({callback.onError(err)})
    }

    private fun onLogin(){
        Logger.getInstance()!!.logI(tag, "Use case finished: logged in successfully"
                , DebugMessage.TYPE_USE_CASE)
        mainHandler.post({callback.onLogin()})
    }

}