package com.pitstop.ui.addCarFragments;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.widget.EditText;

import com.pitstop.R;

/**
 * Created by david on 7/21/2016.
 */
public class AddCarMilageDialog extends DialogFragment {

    AddCarUtils utils;
    public AddCarMilageDialog setCallback(AddCarUtils utils){
        this.utils = utils;
        return this;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(inflater.inflate(R.layout.dialog_milage, null))
                .setTitle("Input Mileage")
                // Add action buttons
                .setPositiveButton("Add Car", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        Dialog dialog2 =Dialog.class.cast(dialog);
                        EditText mileageEditText = (EditText) dialog2.findViewById(R.id.milage);
                        if(TextUtils.isEmpty(mileageEditText.getText().toString())) {
                            utils.callback.hideLoading("Please enter mileage");
                            return;
                        } else if(mileageEditText.getText().toString().length() > 9) {
                            utils.callback.hideLoading("Please enter valid mileage");
                            return;
                        }
                        utils.updateMileage(mileageEditText.getText().toString());
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        AddCarMilageDialog.this.getDialog().cancel();
                    }
                });
        return builder.create();
    }
}
