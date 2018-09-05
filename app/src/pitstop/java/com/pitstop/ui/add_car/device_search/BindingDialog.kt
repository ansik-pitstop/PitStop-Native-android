package com.pitstop.ui.add_car.device_search

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.continental.rvd.mobile_sdk.BindingQuestion
import com.continental.rvd.mobile_sdk.EBindingQuestionType
import com.pitstop.R
import kotlinx.android.synthetic.main.layout_binding_dialog.*

/**
 * Created by Karol Zdebel on 9/5/2018.
 */
class BindingDialog: DialogFragment() {

    private val TAG = BindingDialog::class.java.simpleName

    interface AnswerListener{
        fun onAnswerProvided(answer: String, question: BindingQuestion)
    }

    private var answerListener: AnswerListener? = null
    private var usingEditText = false
    private var question: BindingQuestion? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.layout_binding_dialog,container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        answer_button.setOnClickListener {
            if (question != null){
                answerListener?.onAnswerProvided(
                        if (usingEditText) answer_edit_text.text.toString()
                        else answer_spinner.selectedItem.toString()
                        ,question!!
                )
            }
        }
    }

    fun showProgress(progress: Float){
        Log.d(TAG,"showProgress() progress: $progress")
        progress_bar.progress = (progress * 100).toInt()
    }

    fun showQuestion(question: BindingQuestion){
        Log.d(TAG,"showQuestion() question: $question")
        this.question = question
        instruction.text = question.question

        when (question.questionType){

            EBindingQuestionType.DRIVING_NOT_ALLOWED_CONFIRMATION -> TODO()
            EBindingQuestionType.VIN -> TODO()
            EBindingQuestionType.CAR_MANUFACTURER -> TODO()
            EBindingQuestionType.CAR_MODEL -> TODO()
            EBindingQuestionType.CAR_ENGINE -> TODO()
            EBindingQuestionType.REGIONAL_RESTRICTION -> TODO()
            EBindingQuestionType.OBSERVE_DASHBOARD_LIGHTS -> TODO()
            EBindingQuestionType.CHECK_DASHBOARD_LIGHT -> TODO()
            EBindingQuestionType.LOOKUP -> TODO()
            EBindingQuestionType.ODOMETER -> TODO()
            EBindingQuestionType.BINDING_FEEDBACK -> TODO()
            EBindingQuestionType.INFO_ENGINE_ON_AUTO_ENGINE_OFF -> TODO()
            EBindingQuestionType.IGNITION_OFF -> TODO()
            EBindingQuestionType.ENGINE_ON -> TODO()
            EBindingQuestionType.COUNTRY_OF_INSTALLATION -> TODO()
        }
    }

    fun registerAnswerListener(answerListener: AnswerListener){
        Log.d(TAG,"registerAnswerListener() answerListener: $answerListener")
        this.answerListener = answerListener
    }



}