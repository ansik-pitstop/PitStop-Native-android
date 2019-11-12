package com.pitstop.ui.unit_of_length_dialog

import com.pitstop.utils.UnitOfLength

interface UnitOfLenthDialogView {
    fun setUnitOfLength(unitOfLength: UnitOfLength)
    fun close()
    fun onUnitUpdated()
}