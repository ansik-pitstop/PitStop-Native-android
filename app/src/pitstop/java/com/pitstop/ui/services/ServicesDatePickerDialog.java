package com.pitstop.ui.services;

import android.app.DatePickerDialog;
import android.content.Context;



import java.util.Calendar;

/**
 * Created by Karol Zdebel on 5/24/2017.
 */

public class ServicesDatePickerDialog extends DatePickerDialog{


    public ServicesDatePickerDialog(Context context, Calendar calendar, OnDateSetListener listener) {

        super(context, listener, calendar.get(Calendar.YEAR),calendar.get(Calendar.MONTH),calendar.get(Calendar.DAY_OF_MONTH));
        updateDate(calendar.get(Calendar.YEAR),calendar.get(Calendar.MONTH),calendar.get(calendar.DAY_OF_MONTH));
    }
}
