package com.pitstop.ui.addCarFragments;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.pitstop.R;

/**
 * Created by david on 7/21/2016.
 */
public class AddCarMilageDialog extends DialogFragment {

    AddCarUtils utils;

    public AddCarMilageDialog setCallback(AddCarUtils utils) {
        this.utils = utils;
        return this;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater and get the root view container
        final View rootView = getActivity().getLayoutInflater().inflate(R.layout.dialog_milage, null);
        final EditText mileageEditText = (EditText) rootView.findViewById(R.id.milage);

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        final AlertDialog d = builder.setView(rootView)
                .setTitle("Input Mileage")
                // Add action buttons
//                .setPositiveButton("Add Car", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int id) {
//                        if(TextUtils.isEmpty(mileageEditText.getText().toString())) {
//                            utils.callback.hideLoading("Please enter mileage");
//                        } else if(mileageEditText.getText().toString().length() > 9) {
//                            utils.callback.hideLoading("Please enter valid mileage");
//                        } else{
//                            utils.updateMileage(mileageEditText.getText().toString());
//                        }
//                    }
//                })
                .setPositiveButton("Add Car", null)
//                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int id) {
//                        AddCarMilageDialog.this.getDialog().cancel();
//                    }
//                })
                .setNegativeButton("Cancel", null)
                .create();

        d.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {

                mileageEditText.requestFocus();
                InputMethodManager imm = (InputMethodManager) utils.callback.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);

                d.getButton(DialogInterface.BUTTON_POSITIVE)
                        .setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (TextUtils.isEmpty(mileageEditText.getText().toString())) {
                                    utils.callback.hideLoading("Please enter mileage");
                                } else if (mileageEditText.getText().toString().length() > 9) {
                                    utils.callback.hideLoading("Please enter valid mileage");
                                } else {
                                    InputMethodManager imm = (InputMethodManager) utils.callback.getSystemService(Context.INPUT_METHOD_SERVICE);
                                    imm.hideSoftInputFromWindow(mileageEditText.getWindowToken(), 0);
                                    dismiss();
                                    utils.updateMileage(mileageEditText.getText().toString());
                                }
                            }
                        });

                d.getButton(DialogInterface.BUTTON_NEGATIVE)
                        .setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                AddCarMilageDialog.this.getDialog().cancel();
                            }
                        });
            }
        });

        d.setCanceledOnTouchOutside(false);

        return d;
    }
}
