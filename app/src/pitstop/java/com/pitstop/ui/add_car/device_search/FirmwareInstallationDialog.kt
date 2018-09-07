package com.pitstop.ui.add_car.device_search

import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.pitstop.R
import kotlinx.android.synthetic.main.layout_firmware_installation_dialog.*

/**
 * Created by Karol Zdebel on 9/6/2018.
 */
class FirmwareInstallationDialog: DialogFragment() {

    private var pendingProgress = 0.0f
    private var pendingInstruction = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.layout_firmware_installation_dialog,container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        progress_bar.progress = (pendingProgress*100).toInt()
        instruction.text = pendingInstruction   }

    fun showProgress(progress: Float){
        if (progress_bar == null){
            pendingProgress = progress
        }else{
            progress_bar.progress = (progress*100).toInt()
        }
    }

    fun showError(error: String){
        if (instruction == null){
            pendingInstruction = "Error: $error"
        }else{
            instruction.text = "Error: $error"
        }
    }

    fun showMessage(message: String){
        if (instruction == null){
            pendingInstruction = message
        }else{
            instruction.text = message
        }
    }

    fun closeNextButtonClick(){
        answer_button.setOnClickListener { dismiss() }
    }

    fun setNotClickable(){
        answer_button.setTextColor(Color.GRAY)
        answer_button.isClickable = false
    }

    fun setClickable(){
        answer_button.setTextColor(Color.BLACK)
        answer_button.isClickable = true
    }

    fun registerOkButtonListener(listener: View.OnClickListener){
        answer_button.setOnClickListener(listener)
    }

}