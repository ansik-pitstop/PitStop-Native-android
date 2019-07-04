package com.pitstop.interactors.set

import android.os.Handler
import com.pitstop.repositories.UserRepository

class SetTimezoneUseCaseImpl(val userRepository: UserRepository, val useCaseHandler: Handler, val mainHandler: Handler): SetTimezoneUseCase {
    var callback: SetTimezoneUseCase.Callback? = null

    override fun execute(callback: SetTimezoneUseCase.Callback) {
        this.callback = callback
        useCaseHandler.post(this)
    }

    override fun run() {
        userRepository.updateTimezone()
        callback?.onComplete()
    }
}