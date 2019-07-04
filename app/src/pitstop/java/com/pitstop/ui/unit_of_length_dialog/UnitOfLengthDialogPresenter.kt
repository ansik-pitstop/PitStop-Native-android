package com.pitstop.ui.unit_of_length_dialog

import android.util.Log
import com.pitstop.dependency.UseCaseComponent
import com.pitstop.interactors.get.GetCurrentUserUseCase
import com.pitstop.interactors.set.SetUnitOfLengthUseCase
import com.pitstop.models.User
import com.pitstop.network.RequestError
import com.pitstop.utils.UnitOfLength

class UnitOfLengthDialogPresenter(private val useCaseComponent: UseCaseComponent) {

    private val tag = UnitOfLengthDialogPresenter::class.java.simpleName
    private var view: UnitOfLenthDialogView? = null

    fun subscribe(view: UnitOfLenthDialogView) {
        this.view = view
    }

    fun unsubscribe(){
        Log.d(tag,"unsubscribe()")
        this.view = null
    }

    fun loadCurrentUnit() {
        useCaseComponent.getCurrentUserUseCase.execute(object: GetCurrentUserUseCase.Callback {
            override fun onUserRetrieved(user: User?) {
                if (user == null) return
                if (user.settings.odometer == UnitOfLength.Kilometers.toString()) {
                    view?.setUnitOfLength(UnitOfLength.Kilometers)
                } else {
                    view?.setUnitOfLength(UnitOfLength.Miles)
                }
            }

            override fun onError(error: RequestError?) {
            }
        })
    }

    fun set(unit: UnitOfLength) {
        useCaseComponent.setUnitOfLengthUseCase.execute(unit, object: SetUnitOfLengthUseCase.Callback {
            override fun onError(error: RequestError) {

            }

            override fun onUnitSet(unit: UnitOfLength) {
                Log.d(tag, "onUnitSet() $unit")
                view?.onUnitUpdated()
                view?.close()
            }
        })
    }
}