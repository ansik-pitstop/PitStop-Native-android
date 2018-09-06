package com.pitstop.ui.add_car.device_search

import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.continental.rvd.mobile_sdk.BindingQuestion
import com.continental.rvd.mobile_sdk.EBindingQuestionType
import com.continental.rvd.mobile_sdk.internal.api.binding.model.Error
import com.pitstop.R
import kotlinx.android.synthetic.main.layout_binding_dialog.*

/**
 *
 * Meant to be a debugging and testing solution, not user experience oriented
 * Not meant to be used in production
 *
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
    private var progressBar: ProgressBar? = null
    private var answerButton: Button? = null
    private var cancelButton: Button? = null
    private var backButton: Button? = null
    private var answerSpinner: Spinner? = null
    private var instruction: TextView? = null
    private var answerEditText: EditText? = null

    private var pendingQuestion: BindingQuestion? = null
    private var pendingProgress = 0.0f


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.layout_binding_dialog,container,false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        answerButton = view.findViewById(R.id.answer_button)
        backButton = view.findViewById(R.id.back_button)
        cancelButton = view.findViewById(R.id.cancel_button)
        answerSpinner = view.findViewById(R.id.answer_spinner)
        progressBar = view.findViewById(R.id.progress_bar)
        instruction = view.findViewById(R.id.instruction)
        answerEditText = view.findViewById(R.id.answer_edit_text)

        progressBar?.progress = (pendingProgress * 100).toInt()
        if (pendingQuestion != null){
            showQuestion(pendingQuestion!!)
        }

        answerButton?.setOnClickListener {
            if (question != null){
                answerListener?.onAnswerProvided(
                        when (currentAnswerType) {
                            AnswerType.INPUT -> answerEditText?.text.toString()
                            AnswerType.SELECT -> answerSpinner?.selectedItem.toString()
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
                waitForNextQuestion()
            }
        }
    }

    fun showProgress(progress: Float){
        Log.d(TAG,"showProgress() progress: $progress")
        if (progressBar == null) pendingProgress = progress
        progressBar?.progress = (progress * 100).toInt()
    }

    fun showQuestion(question: BindingQuestion){
        Log.d(TAG,"showQuestion() question: $question")
        this.question = question

        if (instruction == null) this.pendingQuestion = question

        answerButton?.isClickable = true
        answerButton?.setTextColor(Color.BLACK)
        cancelButton?.isClickable = true
        cancelButton?.setTextColor(Color.BLACK)
        backButton?.isClickable = true
        backButton?.setTextColor(Color.BLACK)

        instruction?.text = question.question

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

    fun showFinished(){
        Log.d(TAG,"showFinished()")
        instruction?.text = "Binding process completed!"
        answerButton?.isClickable = true
        answerButton?.setTextColor(Color.BLACK)
        cancelButton?.isClickable = false
        cancelButton?.setTextColor(Color.GRAY)
        backButton?.isClickable = false
        backButton?.setTextColor(Color.GRAY)
    }

    fun showError(error: Error){
        Log.d(TAG,"showError() error: $error")
        instruction?.text = "Error while binding: ${error.message}"
        answerButton?.isClickable = true
        answerButton?.setTextColor(Color.BLACK)
        cancelButton?.isClickable = false
        cancelButton?.setTextColor(Color.GRAY)
        backButton?.isClickable = false
        backButton?.setTextColor(Color.GRAY)
    }

    fun registerAnswerListener(answerListener: AnswerListener){
        Log.d(TAG,"registerAnswerListener() answerListener: $answerListener")
        this.answerListener = answerListener
    }

    private fun waitForNextQuestion(){
        instruction?.text = "Please wait..."
        answerButton?.isClickable = false
        answerButton?.setTextColor(Color.GRAY)
        cancelButton?.isClickable = false
        cancelButton?.setTextColor(Color.GRAY)
        backButton?.isClickable = false
        backButton?.setTextColor(Color.GRAY)

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
                answerEditText?.visibility = View.GONE
                answerSpinner?.visibility = View.GONE
            }
            BindingDialog.AnswerType.INPUT -> {
                answerEditText?.visibility = View.VISIBLE
                answerSpinner?.visibility = View.GONE
            }
            BindingDialog.AnswerType.SELECT -> {
                answerEditText?.visibility = View.GONE
                answerSpinner?.visibility = View.VISIBLE
            }
        }
    }



}