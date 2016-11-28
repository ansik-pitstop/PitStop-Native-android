package com.pitstop.ui.add_car.view_fragment;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.pitstop.R;
import com.pitstop.ui.add_car.AddCarContract;
import com.pitstop.utils.AnimatedDialogBuilder;

/**
 * Mileage input dialog in AddCarActivity
 */
public class AddCarMileageDialog extends DialogFragment {

    AddCarContract.Presenter callback;

    public AddCarMileageDialog setCallback(AddCarContract.Presenter callback) {
        this.callback = callback;
        return this;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AnimatedDialogBuilder builder = new AnimatedDialogBuilder(getActivity());
        // Get the layout inflater and get the root view container
        final View rootView = getActivity().getLayoutInflater().inflate(R.layout.dialog_milage, null);
        final TextInputEditText mileageEditText = (TextInputEditText) rootView.findViewById(R.id.mileage);

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        final AlertDialog d = builder
                .setAnimation(AnimatedDialogBuilder.ANIMATION_GROW)
                .setView(rootView)
                .setTitle("Input Mileage")
                .setPositiveButton("Add Car", null)
                .setNegativeButton("Cancel", null)
                .create();

        d.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                mileageEditText.requestFocus();
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);

                d.getButton(DialogInterface.BUTTON_POSITIVE)
                        .setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                String mileageText = mileageEditText.getText().toString();
                                if (TextUtils.isEmpty(mileageText)) {
                                    Toast.makeText(getActivity(), "Please enter mileage", Toast.LENGTH_SHORT).show();
                                } else if (mileageText.length() > 9) {
                                    Toast.makeText(getActivity(), "Please enter valid mileage", Toast.LENGTH_SHORT).show();
                                } else {
                                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                                    imm.hideSoftInputFromWindow(mileageEditText.getWindowToken(), 0);
                                    dismiss();
                                    callback.updatePendingCarMileage(Integer.parseInt(mileageText));
                                }
                            }
                        });

                d.getButton(DialogInterface.BUTTON_NEGATIVE)
                        .setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                                imm.hideSoftInputFromWindow(mileageEditText.getWindowToken(), 0);
                                dismiss();
                                callback.cancelUpdateMileage();
                            }
                        });
            }
        });

        d.setCanceledOnTouchOutside(false);
        return d;
    }
}
