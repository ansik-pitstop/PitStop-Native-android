package com.pitstop.utils;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.InputType;
import android.util.Log;
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

import org.json.JSONException;
import org.json.JSONObject;

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

    public ServiceRequestUtil(Context context, Car dashboardCar) {
        this.context = context;
        this.dashboardCar = dashboardCar;

        mixpanelHelper = new MixpanelHelper((GlobalApplication) context.getApplicationContext());
        networkHelper = new NetworkHelper(context.getApplicationContext());
    }

    public void start() {
        askForDate();
    }

    private void askForDate() {
        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        final int currentYear = calendar.get(Calendar.YEAR);
        final int currentMonth = calendar.get(Calendar.MONTH);
        final int currentDay = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePicker = new DatePickerDialog(context,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        if(year < currentYear || (year == currentYear
                                && (monthOfYear < currentMonth
                                || (monthOfYear == currentMonth && dayOfMonth < currentDay)))) {
                            Toast.makeText(context, "Please choose a date in the future", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Calendar selectedDate = Calendar.getInstance();
                        selectedDate.set(year, monthOfYear, dayOfMonth);
                        final String dateString = calendar.getTime().toString();

                        askForTime();
                    }
                },
                currentYear,
                currentMonth,
                currentDay);

        TextView titleView = new TextView(context);
        titleView.setText("Please choose a tentative date for service");
        titleView.setBackgroundColor(context.getResources().getColor(R.color.primary_dark));
        titleView.setTextColor(context.getResources().getColor(R.color.white_text));
        titleView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        titleView.setTextSize(18);
        titleView.setPadding(10,10,10,10);

        datePicker.setCustomTitle(titleView);
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

    private void askForTime(int day, int month, int year) {

    }

    private void askForComments(final String dateString) {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
        alertDialog.setTitle("Enter additional comment");

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
                                    + "','Device':'Android','Number of Services Requested':'1'}"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                additionalComment[0] = userInput.getText().toString();
                sendRequest(additionalComment[0], dateString);
            }
        });

        alertDialog.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    mixpanelHelper.trackButtonTapped("Cancel Request Service", TAG);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                dialog.cancel();
            }
        });

        alertDialog.show();
    }

    private void sendRequest(String additionalComment, String date) {
        networkHelper.requestService(((GlobalApplication) context.getApplicationContext()).getCurrentUserId(), dashboardCar.getId(), dashboardCar.getShopId(),
                additionalComment, date, new RequestCallback() {
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
        int maxHour;
        int minHour;

        public LimitedTimePicker(OnTimeSetListener listener) {
            super(context, listener, 8, 0, false);
        }

        @Override
        public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
            super.onTimeChanged(view, hourOfDay, minute);
        }
    }
}
