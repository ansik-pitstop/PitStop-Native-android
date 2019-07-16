package com.pitstop.ui.dialog

import android.util.Log
import com.pitstop.R
import com.pitstop.dependency.UseCaseComponent
import com.pitstop.interactors.get.GetUserCarUseCase
import com.pitstop.interactors.update.UpdateCarMileageUseCase
import com.pitstop.models.Car
import com.pitstop.models.Dealership
import com.pitstop.network.RequestError
import com.pitstop.repositories.Repository

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

    fun loadView(){
        Log.d(tag,"loadView()")
        usecaseComponent.userCarUseCase.execute(Repository.DATABASE_TYPE.LOCAL, object: GetUserCarUseCase.Callback{
            override fun onCarRetrieved(car: Car?, dealership: Dealership?, isLocal: Boolean) {
                if (car != null){
                    view?.showMileage(car.totalMileage.toInt())
                    view?.setEditText(car.totalMileage.toInt().toString())
                }
            }

            override fun onNoCarSet(isLocal: Boolean) {
            }

            override fun onError(error: RequestError?) {
            }

        })
    }

    fun onPositiveButtonClicked(){
        Log.d(tag,"onPositiveButtonClicked()")
        try{
            val mileage = view!!.getMileageInput().toDouble()
            usecaseComponent.updateCarMileageUseCase().execute(mileage, view?.getEventSource(), object: UpdateCarMileageUseCase.Callback{
                override fun onMileageUpdated() {
                    Log.d(tag,"onMileageUpdated()")
                    view?.mileageWasUpdated()
                    view?.closeDialog()
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
            view?.showError(R.string.invalid_mileage_alert_message)
        }
    }

    fun onNegativeButtonCliced(){
        Log.d(tag,"onNegativeButtonClicked()")
        view?.closeDialog()
    }

}