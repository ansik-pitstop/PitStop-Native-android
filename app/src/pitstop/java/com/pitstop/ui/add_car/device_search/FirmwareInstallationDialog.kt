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
    private var okButtonListener: View.OnClickListener? = null
    private var notClickablePending = false
    private var clickablePending = false
    private var pendingCloseNextClick = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.layout_firmware_installation_dialog,container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        progress_bar.progress = (pendingProgress*100).toInt()
        instruction.text = pendingInstruction
        if (okButtonListener != null)
            answer_button.setOnClickListener(okButtonListener)
        if (notClickablePending)
            setNotClickable()
        else if (clickablePending)
            setClickable()
        if (pendingCloseNextClick)
            closeNextButtonClick()
    }

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
        if (answer_button == null){
            pendingCloseNextClick = true
        }else{
            pendingCloseNextClick = false
            setClickable()
            answer_button.setOnClickListener { dismiss() }
        }
    }

    fun setNotClickable(){
        if (answer_button == null){
            notClickablePending = true
        }else{
            notClickablePending = false
            answer_button.setTextColor(Color.LTGRAY)
            answer_button.isClickable = false
        }
    }

    fun setClickable(){
        if (answer_button == null){
            clickablePending = true
        }else{
            clickablePending = false
            answer_button.setTextColor(Color.BLACK)
            answer_button.isClickable = true
        }
    }

    fun registerOkButtonListener(listener: View.OnClickListener){
        if (answer_button == null){
            okButtonListener = listener
        }else{
            answer_button.setOnClickListener(listener)
        }
    }

}