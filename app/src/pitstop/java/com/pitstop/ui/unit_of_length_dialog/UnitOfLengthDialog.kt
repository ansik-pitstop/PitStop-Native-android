package com.pitstop.ui.unit_of_length_dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.pitstop.R
import com.pitstop.dependency.ContextModule
import com.pitstop.dependency.DaggerUseCaseComponent
import com.pitstop.utils.UnitOfLength
import kotlinx.android.synthetic.main.dialog_select_unit.*

interface UnitOfLengthDialogCallback {
    fun onUnitUpdated()
}

class UnitOfLengthDialog: DialogFragment(), UnitOfLenthDialogView {

    var presenter: UnitOfLengthDialogPresenter? = null
    private var callback: UnitOfLengthDialogCallback? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_select_unit, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        unit_cancel_button.setOnClickListener {}
        unit_update_button.setOnClickListener {}
        unit_kilometers.setOnClickListener { setUnitOfLength(UnitOfLength.Kilometers) }
        unit_kilometers_radio.setOnClickListener { setUnitOfLength(UnitOfLength.Kilometers) }

        unit_miles.setOnClickListener { setUnitOfLength(UnitOfLength.Miles) }
        unit_miles_radio.setOnClickListener { setUnitOfLength(UnitOfLength.Miles) }

        unit_cancel_button.setOnClickListener { close() }
        unit_update_button.setOnClickListener { save() }

        presenter = UnitOfLengthDialogPresenter(DaggerUseCaseComponent.builder().contextModule(ContextModule(context)).build())
        presenter?.subscribe(this)
        presenter?.loadCurrentUnit()
    }

    fun setCallback(callback: UnitOfLengthDialogCallback) {
        this.callback = callback
    }

    override fun setUnitOfLength(unitOfLength: UnitOfLength) {
        when(unitOfLength) {
            UnitOfLength.Kilometers -> {
                unit_kilometers_radio.isChecked = true
                unit_miles_radio.isChecked = false
            }
            UnitOfLength.Miles -> {
                unit_miles_radio.isChecked = true
                unit_kilometers_radio.isChecked = false
            }
        }
    }

    fun save() {
        var unit: UnitOfLength? = null
        if (unit_miles_radio.isChecked) {
            unit = UnitOfLength.Miles
        }
        if (unit_kilometers_radio.isChecked) {
            unit = UnitOfLength.Kilometers
        }
        if (unit != null) {
            presenter?.set(unit)
        }
    }

    override fun close() {
        presenter?.unsubscribe()
        dismiss()
    }

    override fun onUnitUpdated() {
        callback?.onUnitUpdated()
    }
}