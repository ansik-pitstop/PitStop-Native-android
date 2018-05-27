package com.pitstop.ui.dialog

import android.util.Log
import com.pitstop.dependency.UseCaseComponent
import com.pitstop.interactors.update.UpdateCarMileageUseCase
import com.pitstop.network.RequestError

/**
 * Created by Karol Zdebel on 5/27/2018.
 */
class MileageDialogPresenter(private val usecaseComponent: UseCaseComponent) {

    private val tag = MileageDialogPresenter::class.java.simpleName

    private var view: MileageDialogView? = null

    fun subscribe(view: MileageDialogView){
        Log.d(tag,"subscribe()")
        this.view = view
    }

    fun unsubscribe(){
        Log.d(tag,"unsubscribe()")
        this.view = null
    }

    fun onPositiveButtonClicked(){
        Log.d(tag,"onPositiveButtonClicked()")
        try{
            val mileage = view!!.getMileageInput().toDouble()
            usecaseComponent.updateCarMileageUseCase().execute(mileage, object: UpdateCarMileageUseCase.Callback{
                override fun onMileageUpdated() {
                    Log.d(tag,"onMileageUpdated()")
                }

                override fun onNoCarAdded() {
                    Log.d(tag,"onNoCarAdded()")
                }

                override fun onError(error: RequestError?) {
                    Log.d(tag,"onError() err: $error")
                }

            })
        }catch(e: NumberFormatException){
            e.printStackTrace()
        }
        view!!.closeDialog()
    }

    fun onNegativeButtonCliced(){
        Log.d(tag,"onNegativeButtonClicked()")
        view?.closeDialog()
    }

}