package com.pitstop.utils;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
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
import com.pitstop.ui.MainActivity;

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

    private static final String VIEW = "Request Service";

    public static final String STATE_TENTATIVE = "tentative";
    public static final String STATE_REQUESTED = "requested";

    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd");
    private SimpleDateFormat timeFormat = new SimpleDateFormat("kk:mm");

    private Context context;
    private Car dashboardCar;

    private MixpanelHelper mixpanelHelper;
    private NetworkHelper networkHelper;

    private final boolean isFirstBooking;

    private Calendar calendar;
    private String timestamp;
    private String comments;

    private final LayoutInflater mLayoutInflater;
    private TextView dateText, timeText;

    private final GlobalApplication mApplication;

    public ServiceRequestUtil(Context context, Car dashboardCar, boolean isFirstBooking) {
        this.context = context;
        this.dashboardCar = dashboardCar;
        this.isFirstBooking = isFirstBooking;

        mixpanelHelper = new MixpanelHelper((GlobalApplication) context.getApplicationContext());
        networkHelper = new NetworkHelper(context.getApplicationContext());
        mLayoutInflater = LayoutInflater.from(context);

        mApplication = (GlobalApplication)context.getApplicationContext();
    }

    /**
     * Prompt user for service information
     */
    public void start() {
        askForDate(false);
    }

    /**
     * Ask for service Date
     * Update the calendar variable with selected date
     *
     * @param modify prompt the user for service time if false, otherwise skip asking for time
     */
    private void askForDate(final boolean modify) {
        calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        final int currentYear = calendar.get(Calendar.YEAR);
        final int currentMonth = calendar.get(Calendar.MONTH);
        final int currentDay = calendar.get(Calendar.DAY_OF_MONTH);

        final int monthToShow = !isFirstBooking ? currentMonth : (currentMonth + 3) % 12;
        final int yearToShow = (isFirstBooking && monthToShow < currentMonth) ? currentYear + 1 : currentYear;

        final LimitedDatePicker datePicker = new LimitedDatePicker(currentDay, currentMonth, currentYear);

        datePicker.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(final DialogInterface dialog) {
                datePicker.updateDate(yearToShow, monthToShow, currentDay);
                datePicker.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (datePicker.selectedYear < currentYear || (datePicker.selectedYear == currentYear
                                && (datePicker.selectedMonth < currentMonth
                                || (datePicker.selectedMonth == currentMonth && datePicker.selectedDay < currentDay)))) {
                            Toast.makeText(context, "Please choose a date in the future", Toast.LENGTH_SHORT).show();
                        } else {
                            dialog.dismiss();
                            calendar.set(datePicker.selectedYear, datePicker.selectedMonth, datePicker.selectedDay);
                            if (modify) {
                                updateDateSelectionView();
                            } else {
                                askForTime(modify);
                            }
                        }
                    }
                });
            }
        });

        if (isFirstBooking) {
            datePicker.setCancelable(false);
            datePicker.setButton(DialogInterface.BUTTON_NEGATIVE, "", (DialogInterface.OnClickListener) null);
        }

        datePicker.setCustomTitle(mLayoutInflater.inflate(R.layout.service_request_dialog_date_picker_title, null));
        datePicker.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                try {
                    mixpanelHelper.trackButtonTapped("Cancel Request Service", VIEW);
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
    private void askForTime(final boolean modify) {
        final LimitedTimePicker timePicker = new LimitedTimePicker(context, null, LimitedTimePicker.MIN_HOUR,
                0, false);

        timePicker.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                timePicker.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (timePicker.selectedHour < LimitedTimePicker.MIN_HOUR || timePicker.selectedHour > LimitedTimePicker.MAX_HOUR
                                || (timePicker.selectedHour == LimitedTimePicker.MAX_HOUR && timePicker.selectedMinute != 0)) {
                            Toast.makeText(context, "Please choose a time between 9:00 AM and 5:00 PM", Toast.LENGTH_SHORT).show();
                        } else {
                            calendar.set(Calendar.HOUR_OF_DAY, timePicker.selectedHour);
                            calendar.set(Calendar.MINUTE, timePicker.selectedMinute);
                            timePicker.dismiss();
                            if (modify) {
                                updateTimeSelectionView();
                            } else {
                                summaryRequest();
                            }
                        }
                    }
                });
            }
        });

        timePicker.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                try {
                    mixpanelHelper.trackButtonTapped("Cancel Request Service", VIEW);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        if (isFirstBooking) {
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
    private void summaryRequest() {

        final AlertDialog.Builder summaryDialogBuilder = new AlertDialog.Builder(context);
        final View view = (mLayoutInflater).inflate(R.layout.dialog_request_service_master, null);

        if (isFirstBooking) {
            //Prompt the user for salesman's name
            ((EditText) view.findViewById(R.id.dialog_service_request_additional_comments)).setHint(R.string.service_request_dialog_additional_comments_hint_2);
        } else {
            ((EditText) view.findViewById(R.id.dialog_service_request_additional_comments)).setHint(R.string.service_request_dialog_additional_comments_hint_1);
        }

        final EditText commentEditText = (EditText) view.findViewById(R.id.dialog_service_request_additional_comments);
        dateText = (TextView) view.findViewById(R.id.dialog_service_request_date_selection);
        timeText = (TextView) view.findViewById(R.id.dialog_service_request_time_selection);

        updateDateSelectionView();
        updateTimeSelectionView();

        //Provide means to modify the date
        view.findViewById(R.id.dialog_service_request_date_card)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        askForDate(true);
                    }
                });

        //Provide means to modify the time
        view.findViewById(R.id.dialog_service_request_time_card)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        askForTime(true);
                    }
                });

        //Create the summary dialog
        summaryDialogBuilder.setView(view)
                .setCustomTitle(mLayoutInflater.inflate(R.layout.service_request_dialog_summary_title, null))
                .setPositiveButton("SEND", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        comments = commentEditText.getText().toString();

                        //format the timestamp before sending the network request because the server use ISO8601 format
                        timestamp = TimestampFormatUtil.format(calendar, TimestampFormatUtil.ISO8601);
                        if (isFirstBooking) {
                            sendRequestWithState(STATE_TENTATIVE, timestamp, comments);
                        } else {
                            sendRequestWithState(STATE_REQUESTED, timestamp, comments);
                        }
                    }
                });

        if (isFirstBooking) {
            summaryDialogBuilder.setCancelable(false)
                    .setNegativeButton("", null);
        } else {
            summaryDialogBuilder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        mixpanelHelper.trackButtonTapped("Cancel Request Service", VIEW);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    Toast.makeText(context, "Service Request Cancelled", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }
            });
        }

        summaryDialogBuilder.create().show();
    }

    /**
     * Update the textview in summary dialog with date selection
     */
    private void updateDateSelectionView() {
        if (dateText != null) {
            dateText.setText(dateFormat.format(calendar.getTime()));
        }
    }

    /**
     * Update the textview in summary dialog with time selection
     */
    private void updateTimeSelectionView() {
        if (timeText != null) {
            timeText.setText(timeFormat.format(calendar.getTime()));
        }
    }

<<<<<<< HEAD
    private void sendRequestWithState(final String state, final String timestamp, final String comments) {
=======
    private void sendRequestWithState(String state, String timestamp, String comments) {

>>>>>>> 05bc644... Clean up
        Log.d("Service Request", "Timestamp: " + timestamp);
        networkHelper.requestService(((GlobalApplication) context.getApplicationContext()).getCurrentUserId(), dashboardCar.getId(),
                dashboardCar.getShopId(), state, timestamp, comments, new RequestCallback() {
                    @Override
                    public void done(String response, RequestError requestError) {
                        if (requestError == null) {
                            try {
                                JSONObject properties = new JSONObject();
                                properties.put("Button", "Confirm Service Request");
                                properties.put("View", VIEW);
                                properties.put("State", isFirstBooking ? "Tentative" : "Requested"); // changes
                                properties.put(isFirstBooking? "Salesperson": "Comments", comments);
                                properties.put("Number of Services Requested", dashboardCar.getActiveIssues().size());
                                mixpanelHelper.trackCustom(MixpanelHelper.EVENT_BUTTON_TAPPED, properties);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            Toast.makeText(context, "Service request sent", Toast.LENGTH_SHORT).show();
                            Smooch.track("User Requested Service");
                            for (CarIssue issue : dashboardCar.getActiveIssues()) {
                                if (issue.getStatus().equals(CarIssue.ISSUE_NEW)) {
                                    networkHelper.servicePending(dashboardCar.getId(), issue.getId(), null);
                                }
                            }
                            if (isFirstBooking){
                                ((MainActivity)context).refreshFromServer();
                            }
                        } else {
                            Log.e(TAG, "service request: " + requestError.getMessage());
                            Toast.makeText(context, "There was an error, please try again", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

    }

    private class LimitedTimePicker extends TimePickerDialog {

        public static final int MAX_HOUR = 17;
        public static final int MIN_HOUR = 9;

        int selectedHour = 9;
        int selectedMinute = 0;

        public LimitedTimePicker(Context context, OnTimeSetListener listener,
                                 int hourOfDay, int minute, boolean is24HourView) {
            super(context, listener, hourOfDay, minute, is24HourView);
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
