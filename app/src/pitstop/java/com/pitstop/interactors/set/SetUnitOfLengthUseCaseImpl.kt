package com.pitstop.interactors.set

import android.os.Handler
import com.pitstop.models.User
import com.pitstop.network.RequestError
import com.pitstop.repositories.Repository
import com.pitstop.repositories.UserRepository
import com.pitstop.utils.UnitOfLength
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

class SetUnitOfLengthUseCaseImpl(val userRepository: UserRepository, val useCaseHandler: Handler, val mainHandler: Handler): SetUnitOfLengthUseCase {

    private var unitOfLength: UnitOfLength? = null
    var callback: SetUnitOfLengthUseCase.Callback? = null
    var bag: Disposable? = null

    override fun execute(unitOfLength: UnitOfLength, callback: SetUnitOfLengthUseCase.Callback) {
        this.unitOfLength = unitOfLength
        this.callback = callback
        useCaseHandler.post(this)
    }

    override fun run() {
        val unitOfLength = this.unitOfLength
        if (unitOfLength != null) {
            userRepository.getCurrentUser(object: Repository.Callback<User> {
                override fun onSuccess(user: User?) {
                    if (user == null) return
                    bag = userRepository.setUnitOfLength(user.id, unitOfLength)
                            .subscribeOn(Schedulers.computation())
                            .observeOn(Schedulers.io())
                            .subscribe { next->
                                callback?.onUnitSet(unitOfLength)
                            }
                }

                override fun onError(error: RequestError?) {
                    if (error != null) callback?.onError(error)
                }
            })
        }

    }

}