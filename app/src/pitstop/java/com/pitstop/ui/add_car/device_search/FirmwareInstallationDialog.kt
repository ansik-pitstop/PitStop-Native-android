package com.pitstop.ui.add_car.device_search

import android.app.DialogFragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.layout_firmware_installation_dialog.*

/**
 * Created by Karol Zdebel on 9/6/2018.
 */
class FirmwareInstallationDialog: DialogFragment() {

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    fun showProgress(progress: Float){
        progress_bar.progress = (progress*100).toInt()
    }

    fun showError(error: String){
        instruction.text = "Error: $error"
    }

    fun showMessage(message: String){
        instruction.text = message
    }

    fun registerOkButtonListener(listener: View.OnClickListener){
        answer_button.setOnClickListener(listener)
    }

}