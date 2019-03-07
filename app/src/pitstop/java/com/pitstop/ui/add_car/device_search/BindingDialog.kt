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
import com.continental.rvd.mobile_sdk.BindingQuestionType
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
        fun onCancelPressed(question: BindingQuestion?)
    }

    interface ShouldStartBindingListener {
        fun onStart()
        fun onCancel()
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
    private var pendingMessage: String = ""
    private var bindingFinished = false

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
        if (!pendingMessage.isEmpty()) {
            instruction?.text = pendingMessage
        }

        answerButton?.setOnClickListener {
            if (question != null) {
                answerListener?.onAnswerProvided(
                        when (currentAnswerType) {
                            AnswerType.INPUT -> answerEditText?.text.toString()
                            AnswerType.SELECT -> {
                                var selectedAnswer = question!!.answers.keys.first()
                                for (answer in question!!.answers) {
                                    if (answer.value == answerSpinner!!.selectedItem) {
                                        selectedAnswer = answer.key
                                        break
                                    }
                                }
                                selectedAnswer
                            }
                            AnswerType.INSTRUCTION -> {
                                BindingQuestion.BIDING_ANSWER_YES
                            }
                        }
                        ,question!!
                )
                waitForNextQuestion()
            } else if (bindingFinished) {
                answerListener?.onCancelPressed(null)
            }
        }

        backButton?.setOnClickListener {
            answerListener?.onAnswerProvided(BindingQuestion.BIDING_ANSWER_YES, question!!)
        }

        cancelButton?.setOnClickListener {
            answerListener?.onCancelPressed(question)
        }
    }

    fun setInstruction(message: String){
        Log.d(TAG,"setInstruction() instruction: $message")
        if (instruction == null){
            pendingMessage = message
        }else{
            instruction?.text = message
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

        updateMessage(question)

        answerButton?.isClickable = true
        answerButton?.setTextColor(Color.BLACK)
        cancelButton?.isClickable = true
        cancelButton?.setTextColor(Color.BLACK)
        backButton?.isClickable = true
        backButton?.setTextColor(Color.BLACK)

        when (question.questionType) {
            BindingQuestionType.VIN,
            BindingQuestionType.ODOMETER -> {
                toggleAnswer(AnswerType.INPUT)
            }
            BindingQuestionType.CAR_MANUFACTURER,
            BindingQuestionType.CAR_MODEL,
            BindingQuestionType.LOOKUP,
            BindingQuestionType.CAR_ENGINE -> {
                toggleAnswer(AnswerType.SELECT)
                populateSpinner(question.answers)
            }

            BindingQuestionType.OBSERVE_DASHBOARD_LIGHTS,
            BindingQuestionType.DRIVING_NOT_ALLOWED_CONFIRMATION,
            BindingQuestionType.IGNITION_OFF,
            BindingQuestionType.ENGINE_ON,
            BindingQuestionType.INFO_ENGINE_ON_AUTO_ENGINE_OFF,
            BindingQuestionType.BINDING_FEEDBACK,
            BindingQuestionType.CHECK_DASHBOARD_LIGHT,
            BindingQuestionType.REGIONAL_RESTRICTION -> {
                toggleAnswer(AnswerType.INSTRUCTION)
            }

            else -> {
                Log.d(TAG, "Invalid question type.")
            }
        }
    }

    private fun updateMessage(question: BindingQuestion) {
        if (question.question == null || question.question.isEmpty()) {
            val instructionText = when(question.questionType) {
                BindingQuestionType.VIN -> "Insert your VIN number"
                BindingQuestionType.ODOMETER -> "Insert your odometer value"
                BindingQuestionType.CAR_MANUFACTURER -> "Select your car manufacturer"
                BindingQuestionType.LOOKUP -> "Select your a lookup"
                BindingQuestionType.CAR_MODEL -> "Select your car model"
                BindingQuestionType.CAR_ENGINE -> "Select your car engine"
                BindingQuestionType.OBSERVE_DASHBOARD_LIGHTS -> "Observe your warning signs on the car dashboard"
                BindingQuestionType.DRIVING_NOT_ALLOWED_CONFIRMATION -> "Driving is not allowed during binding process"
                BindingQuestionType.IGNITION_OFF -> "Please turn your ignition OFF"
                BindingQuestionType.ENGINE_ON -> "Please turn your ignition ON"
                BindingQuestionType.INFO_ENGINE_ON_AUTO_ENGINE_OFF -> "Your engine should be ON during the binding process"
                BindingQuestionType.BINDING_FEEDBACK -> "Was the binding process good?"
                BindingQuestionType.CHECK_DASHBOARD_LIGHT -> "Did you notice any warning signs?"
                BindingQuestionType.REGIONAL_RESTRICTION -> "Unfortunately not all functionalities will be available to you"
                BindingQuestionType.COUNTRY_OF_INSTALLATION-> "Unfortunately not all that will; be available to you"
            }

            instruction?.text = instructionText
        } else {
            instruction?.text = question.question
        }
    }

    fun showFinished(){
        Log.d(TAG,"showFinished()")
        bindingFinished = true
        instruction?.text = "Binding process completed!"
        answerButton?.isClickable = true
        answerButton?.setTextColor(Color.BLACK)
        answerButton?.text = "Close binding prosses"
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
        toggleAnswer(AnswerType.INSTRUCTION)
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
                , answers.values.toTypedArray())
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