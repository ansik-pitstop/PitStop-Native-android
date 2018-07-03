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
                SmoochUtil.setSmoochProperties(user)
                if (user.settings.hasMainCar()){
                    val disposable = carRepository.get(user.settings.carId, Repository.DATABASE_TYPE.REMOTE)
                            .subscribeOn(Schedulers.computation())
                            .observeOn(Schedulers.io(), true)
                            .subscribe({next->
                                val car: Car = next.data!!
                                //Set car
                                SmoochUtil.setSmoochProperties(car)
                                Log.d(tag,"set smooch user proerties! user: $user")
                            }, {error ->
                                Log.e(tag,"error storing custom properties! err: $error")
                            })
                    compositeDisposable.add(disposable)
                }

                try {
                    val call = smoochApi.getSmoochToken(userId).execute()
                    if (call.isSuccessful) {
                        Log.d(tag, "call successful")
                        val body = call.body()
                        if (body != null) {
                            val smoochToken = body.get("smoochToken").asString
                            Log.d(tag, "smooch token: " + smoochToken)
                            if (smoochToken != null) {
                                Smooch.login(userId.toString(), smoochToken, {
                                    Log.d(tag, "login response err: " + it.error)
                                    if (it.error == null) onLogin()
                                    else onErrorFound(RequestError.getUnknownError())
                                })
                            } else {
                                Log.d(tag, "err smooch token null")
                                onErrorFound(RequestError.getUnknownError())
                            }
                        } else {
                            Log.d(tag, "err body null")
                            onErrorFound(RequestError.getUnknownError())
                        }
                    } else {
                        Log.d(tag, "call unsuccessful")
                        onErrorFound(RequestError.getUnknownError())
                    }
                }catch(e: IOException){
                    onErrorFound(RequestError.getOfflineError())
                }
            }

            override fun onError(error: RequestError) {
                onErrorFound(error)
            }

        })
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