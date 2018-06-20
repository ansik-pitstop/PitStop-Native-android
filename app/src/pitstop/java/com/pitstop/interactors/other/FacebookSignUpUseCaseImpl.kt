package com.pitstop.interactors.other

import android.os.Bundle
import android.os.Handler
import android.util.Log
import com.facebook.AccessToken
import com.facebook.GraphRequest
import com.pitstop.models.User
import com.pitstop.network.RequestError
import com.pitstop.repositories.UserRepository
import io.reactivex.schedulers.Schedulers

/**
 * Created by Karol Zdebel on 6/19/2018.
 */
class FacebookSignUpUseCaseImpl(private val userRepository: UserRepository
                                , private val loginManager: com.pitstop.utils.LoginManager
                                , private val useCaseHandler: Handler
                                ,private val mainHandler: Handler): FacebookSignUpUseCase {

    private val TAG = FacebookSignUpUseCase::class.java.simpleName

    private lateinit var callback: FacebookSignUpUseCase.Callback

    override fun execute(callback: FacebookSignUpUseCase.Callback) {
        this.callback = callback
        useCaseHandler.post(this)
    }

    override fun run() {
        if (AccessToken.getCurrentAccessToken() == null){
            onError(RequestError.getUnknownError())
            return
        }
        val request = GraphRequest.newMeRequest(AccessToken.getCurrentAccessToken()) { responseObject, response ->
            try {
                // Application code
                Log.d(TAG,"response: $response, responseObject: $responseObject")
                val email = response?.jsonObject?.getString("email")
                val name = response?.jsonObject?.getString("name")
                val firstName = name?.split(" ")?.get(0)
                val lastName = name?.split(" ")?.get(1)

                val user = User()
                user.email = email
                user.firstName = firstName
                user.lastName = lastName
                user.phone = ""
                user.password = ""

                userRepository.insert(user,true)
                        .subscribeOn(Schedulers.computation())
                        .observeOn(Schedulers.io())
                        .subscribe({next ->
                            Log.d(TAG,"next: $next")
                            userRepository.loginFacebook(AccessToken.getCurrentAccessToken().token)
                                    .subscribeOn(Schedulers.computation())
                                    .observeOn(Schedulers.io()).subscribe({user->
                                        Log.d(TAG,"login user response: $user")
                                        loginManager.loginUser(user.accessToken,user.refreshToken,user.user)
                                        SignUpUseCaseImpl@onSuccess()
                                    },{err ->
                                        Log.d(TAG,"login user error: $err")
                                        SignUpUseCaseImpl@onError(RequestError(err))
                                    })
                        },{err ->
                            Log.d(TAG,"err: $err")
                            FacebookSignUpUseCaseImpl@onError(RequestError(err))
                        })

                Log.d(TAG,"got facebook email: $email")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        val parameters = Bundle()
        parameters.putString("fields","id,name,email,gender,birthday")
        request.parameters = parameters
        request.executeAsync()
    }

    private fun onSuccess(){

        mainHandler.post({callback.onSuccess()})
    }

    private fun onError(error: RequestError){

        mainHandler.post({callback.onError(error)})
    }
}