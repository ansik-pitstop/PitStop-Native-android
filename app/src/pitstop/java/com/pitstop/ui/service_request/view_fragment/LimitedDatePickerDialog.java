package com.pitstop.ui.service_request.view_fragment;

import android.app.DatePickerDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TextView;

import com.pitstop.R;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by yifan on 16/11/24.
 */

public class LimitedDatePickerDialog extends DatePickerDialog {

    private static final String TAG = LimitedDatePickerDialog.class.getSimpleName();

    public int selectedYear = 0;
    public int selectedMonth = 0;
    public int selectedDay = 0;

    public LimitedDatePickerDialog(Context context, OnDateSetListener listener, int year, int month, int day) {
        super(context, listener, year, month, day);
        selectedYear = year;
        selectedMonth = month;
        selectedDay = day;
    }

    @Override
    public void onDateChanged(DatePicker view, int year, int month, int day) {
        super.onDateChanged(view, year, month, day);
        selectedDay = day;
        selectedMonth = month;
        selectedYear = year;
    }

    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);
        View customTitle = LayoutInflater.from(getContext()).inflate(R.layout.dialog_custom_title_primary_dark, null);
        ((TextView)customTitle.findViewById(R.id.custom_title_text)).setText(title);
        setCustomTitle(customTitle);
    }

    public boolean isValidDate(){
        Calendar calendar = Calendar.getInstance();
        Date today = calendar.getTime();

        calendar.set(Calendar.YEAR, selectedYear);
        calendar.set(Calendar.MONTH, selectedMonth);
        calendar.set(Calendar.DAY_OF_MONTH, selectedDay);
        Date selectedDay = calendar.getTime();

        return selectedDay.after(today) || selectedDay.equals(today);
    }
}
