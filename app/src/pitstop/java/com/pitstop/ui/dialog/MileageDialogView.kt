package com.pitstop.ui.dialog

/**
 * Created by Karol Zdebel on 5/27/2018.
 */
interface MileageDialogView {
    fun getMileageInput(): String
    fun closeDialog()
    fun showMileage(mileage: String)
}