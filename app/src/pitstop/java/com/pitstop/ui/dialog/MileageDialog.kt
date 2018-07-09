package com.pitstop.ui.dialog

import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.pitstop.EventBus.EventSource
import com.pitstop.EventBus.EventSourceImpl
import com.pitstop.R
import com.pitstop.dependency.ContextModule
import com.pitstop.dependency.DaggerUseCaseComponent
import kotlinx.android.synthetic.main.dialog_milage.*


/**
 * Created by Karol Zdebel on 5/24/2018.
 */
class MileageDialog: DialogFragment(),MileageDialogView {

    private var presenter: MileageDialogPresenter? = null
    private var eventSource: EventSource = EventSourceImpl(EventSource.SOURCE_MY_GARAGE)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout to use as dialog or embedded fragment

        presenter = MileageDialogPresenter(DaggerUseCaseComponent.builder()
                .contextModule(ContextModule(context))
                .build())

        val view = inflater.inflate(R.layout.dialog_milage, container, false)
        return view
    }

    override fun getEventSource(): EventSource {
        return eventSource
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        button_negative.setOnClickListener({presenter?.onNegativeButtonCliced()})
        button_positive.setOnClickListener({presenter?.onPositiveButtonClicked()})
        presenter?.loadView()
    }

    override fun showMileage(mileage: Int) {
        mileage_text_view?.text = String.format(resources.getText(R.string.mileage_dialog_string).toString(),mileage)
    }

    override fun setEditText(text: String) {
        editText?.setText(text)
    }

    override fun onStart() {
        presenter?.subscribe(this)
        super.onStart()
    }

    override fun onStop() {
        presenter?.unsubscribe()
        super.onStop()
    }

    override fun getMileageInput(): String{
        return editText.text.toString()
    }

    override fun closeDialog() {
        dismiss()
    }

    override fun onDismiss(dialog: DialogInterface?) {
        presenter?.unsubscribe()
        super.onDismiss(dialog)
    }

    override fun showError(err: Int) {
        error_text?.text = resources.getText(err)
    }
}