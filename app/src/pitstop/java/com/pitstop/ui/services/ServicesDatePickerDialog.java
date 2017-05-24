package com.pitstop.ui.services;

import android.app.DatePickerDialog;
import android.content.Context;

import com.pitstop.ui.service_request.view_fragment.LimitedDatePickerDialog;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by Karol Zdebel on 5/24/2017.
 */

public class ServicesDatePickerDialog extends DatePickerDialog{
    private static final String TAG = LimitedDatePickerDialog.class.getSimpleName();

    public int selectedYear = 0;
    public int selectedMonth = 0;
    public int selectedDay = 0;

    public ServicesDatePickerDialog(Context context, OnDateSetListener listener) {

        super(context, listener, Calendar.getInstance().YEAR
                , Calendar.getInstance().MONTH, Calendar.getInstance().DAY_OF_MONTH);

        selectedYear = Calendar.getInstance().YEAR;
        selectedMonth = Calendar.getInstance().MONTH;
        selectedDay = Calendar.getInstance().DAY_OF_MONTH;
    }

    public boolean isValidDate(){
        Calendar calendar = Calendar.getInstance();
        Date today = calendar.getTime();

        calendar.set(Calendar.YEAR, selectedYear);
        calendar.set(Calendar.MONTH, selectedMonth);
        calendar.set(Calendar.DAY_OF_MONTH, selectedDay);
        Date selectedDay = calendar.getTime();

        return selectedDay.before(today) || selectedDay.equals(today);
    }

}
