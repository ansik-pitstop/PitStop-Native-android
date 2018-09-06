package com.pitstop.ui.add_car.device_search

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.continental.rvd.mobile_sdk.BindingQuestion
import com.continental.rvd.mobile_sdk.EBindingQuestionType
import com.pitstop.R
import kotlinx.android.synthetic.main.layout_binding_dialog.*

/**
 * Created by Karol Zdebel on 9/5/2018.
 */
class BindingDialog: DialogFragment() {

    private val TAG = BindingDialog::class.java.simpleName

    enum class AnswerType {
        INSTRUCTION, INPUT, SELECT
    }

    interface AnswerListener{
        fun onAnswerProvided(answer: String, question: BindingQuestion)
        fun onBackPressed(question: BindingQuestion)
        fun onCancelPressed(question: BindingQuestion)
    }

    private var answerListener: AnswerListener? = null
    private var currentAnswerType = AnswerType.INPUT
    private var question: BindingQuestion? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.layout_binding_dialog,container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        answer_button.setOnClickListener {
            if (question != null){
                answerListener?.onAnswerProvided(
                        when (currentAnswerType) {
                            AnswerType.INPUT -> answer_edit_text.text.toString()
                            AnswerType.SELECT -> answer_spinner.selectedItem.toString()
                            else -> ""
                        }
                        ,question!!
                )

                back_button.setOnClickListener {
                    answerListener?.onBackPressed(question!!)
                }
                cancel_button.setOnClickListener {
                    answerListener?.onCancelPressed(question!!)
                }

                //Hide input fields till next question arrives
                toggleAnswer(AnswerType.INSTRUCTION)
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

            EBindingQuestionType.DRIVING_NOT_ALLOWED_CONFIRMATION ->{
                toggleAnswer(AnswerType.INSTRUCTION)
            }
            EBindingQuestionType.VIN -> {
                toggleAnswer(AnswerType.INPUT)
            }
            EBindingQuestionType.CAR_MANUFACTURER -> {
                toggleAnswer(AnswerType.SELECT)
                populateSpinner(question.answers)
            }
            EBindingQuestionType.CAR_MODEL -> {
                toggleAnswer(AnswerType.SELECT)
                populateSpinner(question.answers)
            }
            EBindingQuestionType.CAR_ENGINE ->{
                toggleAnswer(AnswerType.SELECT)
                populateSpinner(question.answers)
            }
            EBindingQuestionType.REGIONAL_RESTRICTION -> {
                toggleAnswer(AnswerType.INSTRUCTION)
            }
            EBindingQuestionType.OBSERVE_DASHBOARD_LIGHTS ->{
                toggleAnswer(AnswerType.INSTRUCTION)
            }
            EBindingQuestionType.CHECK_DASHBOARD_LIGHT ->{
                toggleAnswer(AnswerType.INSTRUCTION)
            }
            EBindingQuestionType.LOOKUP -> {
                toggleAnswer(AnswerType.INSTRUCTION)
            }
            EBindingQuestionType.ODOMETER ->{
                toggleAnswer(AnswerType.INPUT)
            }
            EBindingQuestionType.BINDING_FEEDBACK -> {
                toggleAnswer(AnswerType.INSTRUCTION)
            }
            EBindingQuestionType.INFO_ENGINE_ON_AUTO_ENGINE_OFF -> {
                toggleAnswer(AnswerType.INSTRUCTION)
            }
            EBindingQuestionType.IGNITION_OFF -> {
                toggleAnswer(AnswerType.INSTRUCTION)
            }
            EBindingQuestionType.ENGINE_ON -> {
                toggleAnswer(AnswerType.INSTRUCTION)
            }
            EBindingQuestionType.COUNTRY_OF_INSTALLATION -> {
                toggleAnswer(AnswerType.INSTRUCTION)
            }
        }
    }

    fun registerAnswerListener(answerListener: AnswerListener){
        Log.d(TAG,"registerAnswerListener() answerListener: $answerListener")
        this.answerListener = answerListener
    }

    private fun populateSpinner(answers: Map<String,String>){
        val arrayAdapter = ArrayAdapter(activity!!
                , android.R.layout.simple_spinner_dropdown_item
                , answers.keys.toTypedArray())
        answer_spinner.adapter = arrayAdapter
    }

    private fun toggleAnswer(answerType: AnswerType){
        currentAnswerType = answerType
        when (answerType){
            BindingDialog.AnswerType.INSTRUCTION -> {
                answer_edit_text.visibility = View.GONE
                answer_spinner.visibility = View.GONE
            }
            BindingDialog.AnswerType.INPUT -> {
                answer_edit_text.visibility = View.VISIBLE
                answer_spinner.visibility = View.GONE
            }
            BindingDialog.AnswerType.SELECT -> {
                answer_edit_text.visibility = View.GONE
                answer_spinner.visibility = View.VISIBLE
            }
        }
    }



}