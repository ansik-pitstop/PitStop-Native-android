package com.pitstop.ui.services;

import android.app.DatePickerDialog;
import android.content.Context;

import com.pitstop.ui.service_request.view_fragment.LimitedDatePickerDialog;

/**
 * Created by Karol Zdebel on 5/24/2017.
 */

public class ServicesDatePickerDialog extends DatePickerDialog{
    private static final String TAG = LimitedDatePickerDialog.class.getSimpleName();

    public ServicesDatePickerDialog(Context context, OnDateSetListener listener, int year, int month, int day) {

        super(context, listener, year,month,day);
        updateDate(year,month,day);
    }
}
