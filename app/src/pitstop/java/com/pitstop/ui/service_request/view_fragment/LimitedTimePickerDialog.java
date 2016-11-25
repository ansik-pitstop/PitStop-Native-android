package com.pitstop.ui.service_request.view_fragment;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.TimePicker;

import com.pitstop.R;


public class LimitedTimePickerDialog extends TimePickerDialog {

    private static final String TAG = LimitedTimePickerDialog.class.getSimpleName();

    public static final int MAX_HOUR = 17;
    public static final int MAX_MINUTE = 0;
    public static final int MAX_TIME = MAX_HOUR * 60 + MAX_MINUTE;

    public static final int MIN_HOUR = 7;
    public static final int MIN_MINUTE = 30;
    public static final int MIN_TIME = MIN_HOUR * 60 + MIN_MINUTE;

    public int selectedHour = 7;
    public int selectedMinute = 30;

    public LimitedTimePickerDialog(Context context, OnTimeSetListener listener,
                      int hourOfDay, int minute, boolean is24HourView) {
        super(context, listener, hourOfDay, minute, is24HourView);
    }

    @Override
    public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
        super.onTimeChanged(view, hourOfDay, minute);

        selectedHour = hourOfDay;
        selectedMinute = minute;
    }

    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);
        View customTitle = LayoutInflater.from(getContext()).inflate(R.layout.dialog_custom_title_primary_dark, null);
        ((TextView)customTitle.findViewById(R.id.custom_title_text)).setText(title);
        setCustomTitle(customTitle);
    }

    /**
     * @return true if valid
     */
    public boolean isValidTime(){
        int time = selectedHour * 60 + selectedMinute;
        Log.d(TAG, "Selected time: " + time);
        Log.d(TAG, "Max: " + MAX_TIME);
        Log.d(TAG, "Min: " + MIN_TIME);
        return time >= MIN_TIME && time <= MAX_TIME;
    }
}
