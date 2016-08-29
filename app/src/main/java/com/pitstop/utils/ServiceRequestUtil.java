package com.pitstop.utils;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.widget.CardView;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.models.Car;
import com.pitstop.models.CarIssue;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import io.smooch.core.Smooch;

/**
 * Created by Ben Wu on 2016-08-24.
 */
public class ServiceRequestUtil {

    private static final String TAG = ServiceRequestUtil.class.getSimpleName();

    private Context context;
    private Car dashboardCar;

    private MixpanelHelper mixpanelHelper;
    private NetworkHelper networkHelper;

    private final boolean isFirstBooking;

    private Calendar calendar;
    private String dateTime;
    private String comments;

    private final LayoutInflater mLayoutInflater;

    public ServiceRequestUtil(Context context, Car dashboardCar, boolean isFirstBooking) {
        this.context = context;
        this.dashboardCar = dashboardCar;
        this.isFirstBooking = isFirstBooking;

        mixpanelHelper = new MixpanelHelper((GlobalApplication) context.getApplicationContext());
        networkHelper = new NetworkHelper(context.getApplicationContext());
        mLayoutInflater = LayoutInflater.from(context);
    }

    /**
     * Prompt user for service information
     */
    public void start() {
        askForDate();
//        askForTime();
//        summaryRequest();
    }

    /**
     * Ask for service Date
     * Update the calendar variable with selected date
     */
    private void askForDate() {
        calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        final int currentYear = calendar.get(Calendar.YEAR);
        final int currentMonth = calendar.get(Calendar.MONTH);
        final int currentDay = calendar.get(Calendar.DAY_OF_MONTH);

        final int monthToShow = !isFirstBooking ? calendar.get(Calendar.MONTH)
                : (calendar.get(Calendar.MONTH) + 3) % 12; // 3 months in future for first booking

        final LimitedDatePicker datePicker = new LimitedDatePicker(currentDay, currentMonth, currentYear);

        datePicker.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(final DialogInterface dialog) {
                datePicker.updateDate(currentYear, monthToShow, currentDay);
                datePicker.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(datePicker.selectedYear < currentYear || (datePicker.selectedYear == currentYear
                                && (datePicker.selectedMonth < currentMonth
                                || (datePicker.selectedMonth == currentMonth && datePicker.selectedDay < currentDay)))) {
                            Toast.makeText(context, "Please choose a date in the future", Toast.LENGTH_SHORT).show();
                        } else {
                            dialog.dismiss();
                            calendar.set(datePicker.selectedYear, datePicker.selectedMonth, datePicker.selectedDay);
                            askForTime();
//                            askForTime(datePicker.selectedDay, datePicker.selectedMonth, datePicker.selectedYear);
                        }
                    }
                });
            }
        });

        if(isFirstBooking) {
            datePicker.setCancelable(false);
            datePicker.setButton(DialogInterface.BUTTON_NEGATIVE, "", (DialogInterface.OnClickListener) null);
        }

        datePicker.setCustomTitle(mLayoutInflater.inflate(R.layout.service_request_dialog_date_picker_title, null));
        datePicker.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                try {
                    mixpanelHelper.trackButtonTapped("Cancel Request Service", TAG);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        datePicker.show();
    }

    /**
     * Ask for service time
     * Will be called by method askForDate() after date is successfully prompted
     * Update the calendar variable with selected time,
     * before this, time is by default set to current time
     */
    private void askForTime() {
        final int maxHour = 17;
        final int minHour = 8;

        final LimitedTimePicker timePicker = new LimitedTimePicker();

        timePicker.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                timePicker.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(timePicker.selectedHour < minHour || timePicker.selectedHour > maxHour
                                || (timePicker.selectedHour == maxHour && timePicker.selectedMinute != 0)) {
                            Toast.makeText(context, "Please choose a time between 8:00 AM and 5:00 PM", Toast.LENGTH_SHORT).show();
                        } else {
                            calendar.set(Calendar.HOUR_OF_DAY, timePicker.selectedHour);
                            calendar.set(Calendar.MINUTE, timePicker.selectedMinute);
                            timePicker.dismiss();
                            Log.d(TAG, calendar.getTime().toString() + " TIME HERE");
                            summaryRequest();
                        }
                    }
                });
            }
        });

        timePicker.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                try {
                    mixpanelHelper.trackButtonTapped("Cancel Request Service", TAG);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        if(isFirstBooking) {
            timePicker.setCancelable(false);
            timePicker.setButton(DialogInterface.BUTTON_NEGATIVE, "", (DialogInterface.OnClickListener) null);
        }

        timePicker.setCustomTitle(mLayoutInflater.inflate(R.layout.service_request_dialog_time_picker_title, null));

        timePicker.show();
    }

    /**
     * Ask for user's additional comments after getting the datetime
     * Also summary the request (date, time, comments) making it intuitive
     * After user confirms, this request will be sent
     */
    private void summaryRequest(){
        final AlertDialog.Builder summaryDialog = new AlertDialog.Builder(context);
        View view = (mLayoutInflater).inflate(R.layout.dialog_request_service_master, null);

        final EditText commentEditText = (EditText) view.findViewById(R.id.dialog_service_request_additional_comments);
        TextView dateText = (TextView)view.findViewById(R.id.dialog_service_request_date_selection);
        TextView timeText = (TextView)view.findViewById(R.id.dialog_service_request_time_selection);

        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd");
        SimpleDateFormat timeFormat = new SimpleDateFormat("kk:mm");

        String date = dateFormat.format(calendar.getTime());
        String time = timeFormat.format(calendar.getTime());

        dateText.setText(date);
        timeText.setText(time);

        //Provide means to modify the date
        view.findViewById(R.id.dialog_service_request_date_card)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        askForDate();
                    }
                });

        //Provide means to modify the time
        view.findViewById(R.id.dialog_service_request_time_card)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        askForTime();
                    }
                });

        //Create the summary dialog
        summaryDialog.setView(view)
                .setCustomTitle(mLayoutInflater.inflate(R.layout.service_request_dialog_summary_title, null))
                .setPositiveButton("SEND", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            mixpanelHelper.trackCustom("Button Tapped",
                                    new JSONObject("{'Button':'Confirm Service Request','View':'" + TAG
                                            + "','Device':'Android','Number of Services Requested':'" + dashboardCar.getActiveIssues().size() + "'}"));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        comments = commentEditText.getText().toString();
                        dateTime = calendar.getTime().toString();
                        sendRequest(comments, dateTime);
                    }
                });
        if (isFirstBooking){
            summaryDialog.setCancelable(false)
                    .setNegativeButton("", null);
        } else{
            summaryDialog.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        mixpanelHelper.trackButtonTapped("Cancel Request Service", TAG);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    Toast.makeText(context, "Service Request Cancelled", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }
            });
        }
        summaryDialog.create().show();

    }

    /**
     * Left unused after refactoring
     */
    private void askForComments() {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
        alertDialog.setTitle("Enter additional comments");

        final String[] additionalComment = {""};
        final EditText userInput = new EditText(context);
        userInput.setInputType(InputType.TYPE_CLASS_TEXT);
        alertDialog.setView(userInput);

        alertDialog.setPositiveButton("SEND", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    mixpanelHelper.trackCustom("Button Tapped",
                            new JSONObject("{'Button':'Confirm Service Request','View':'" + TAG
                                    + "','Device':'Android','Number of Services Requested':'" + dashboardCar.getActiveIssues().size() + "'}"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                additionalComment[0] = userInput.getText().toString();
                comments = userInput.getText().toString();
//                sendRequest(additionalComment[0], dateString);
            }
        });

        if(!isFirstBooking) {
            alertDialog.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        mixpanelHelper.trackButtonTapped("Cancel Request Service", TAG);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    dialog.dismiss();
                }
            });
        } else {
            alertDialog.setCancelable(false);
        }

        alertDialog.show();
    }

    /**
     * Based on the given date time value and user comment,
     * sends network request to request service
     * @param additionalComment
     * @param date
     */
    private void sendRequest(String additionalComment, String date) {
        networkHelper.requestService(((GlobalApplication) context.getApplicationContext()).getCurrentUserId(), dashboardCar.getId(), dashboardCar.getShopId(),
                additionalComment, date, isFirstBooking, new RequestCallback() {
                    @Override
                    public void done(String response, RequestError requestError) {
                        if(requestError == null) {
                            Toast.makeText(context, "Service request sent", Toast.LENGTH_SHORT).show();
                            Smooch.track("User Requested Service");
                            for(CarIssue issue : dashboardCar.getActiveIssues()) {
                                if(issue.getStatus().equals(CarIssue.ISSUE_NEW)) {
                                    networkHelper.servicePending(dashboardCar.getId(), issue.getId(), null);
                                }
                            }
                        } else {
                            Log.e(TAG, "service request: " + requestError.getMessage());
                            Toast.makeText(context, "There was an error, please try again", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private class LimitedTimePicker extends TimePickerDialog {
        int selectedHour = 8;
        int selectedMinute = 0;

        public LimitedTimePicker() {
            super(context, null, 8, 0, false);
        }

        @Override
        public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
            super.onTimeChanged(view, hourOfDay, minute);

            selectedHour = hourOfDay;
            selectedMinute = minute;
        }
    }

    private class LimitedDatePicker extends DatePickerDialog {
        int selectedYear = 0;
        int selectedMonth = 0;
        int selectedDay = 0;

        public LimitedDatePicker(int day, int month, int year) {
            super(context, null, year, month, year);
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
    }




}
