package com.pitstop.utils;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.design.widget.TextInputEditText;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.models.Car;
import com.pitstop.models.CarIssue;
import com.pitstop.models.CarIssuePreset;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.ui.MainActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import io.smooch.core.Smooch;

/**
 * Created by Ben Wu on 2016-08-24. <br>
 * This class provides two basic endpoint: <br>
 * {@link #startBookingService(boolean chained)}, which is used to request service<br>
 * {@link #startAddingPresetIssues(boolean chained)}, which is used to add preset issues <br>
 *
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

        mApplication = (GlobalApplication) context.getApplicationContext();
    }

    /**
     * Prompt the user to book service appointment
     * @param chained True if this method is invoked in Add Custom Issues process, otherwise false
     */
    public void startBookingService(final boolean chained) {
        askForDate(false, chained);
    }

    /**
     * Ask for service Date
     * Update the calendar variable with selected date
     *
     * @param modify prompt the user for service time if false, otherwise skip asking for time
     */
    private void askForDate(final boolean modify, final boolean chained) {
        calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        final int currentYear = calendar.get(Calendar.YEAR);
        final int currentMonth = calendar.get(Calendar.MONTH);
        final int currentDay = calendar.get(Calendar.DAY_OF_MONTH);

        final int monthToShow = !isFirstBooking ? currentMonth : (currentMonth + 3) % 12;
        final int yearToShow = (isFirstBooking && monthToShow < currentMonth) ? currentYear + 1 : currentYear;

        final LimitedDatePicker datePicker = new LimitedDatePicker(currentDay, currentMonth, currentYear);

        datePicker.setCanceledOnTouchOutside(!isFirstBooking);
        datePicker.setCancelable(!isFirstBooking);

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
                                askForTime(modify, chained);
                            }
                        }
                    }
                });

                if (isFirstBooking){
                    datePicker.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            new AnimatedDialogBuilder(context)
                                    .setAnimation(AnimatedDialogBuilder.ANIMATION_GROW)
                                    .setTitle(context.getString(R.string.first_service_booking_cancel_title))
                                    .setMessage(context.getString(R.string.first_service_booking_cancel_message))
                                    .setNegativeButton("Continue booking", null) // Do nothing on continue
                                    .setPositiveButton("Quit booking", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                            datePicker.cancel();
                                        }
                                    })
                                    .show();
                        }
                    });
                }
            }
        });


        final View customTitle = mLayoutInflater.inflate(R.layout.dialog_custom_title_primary_dark, null, false);
        ((TextView) customTitle.findViewById(R.id.custom_title_text)).setText(context.getString(R.string.service_request_dialog_date_picker_title));
        datePicker.setCustomTitle(customTitle);

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
    private void askForTime(final boolean modify, final boolean chained) {
        final LimitedTimePicker timePicker = new LimitedTimePicker(context, null, LimitedTimePicker.MIN_HOUR,
                LimitedTimePicker.MIN_MINUTE, false);

        timePicker.setCanceledOnTouchOutside(!isFirstBooking);
        timePicker.setCancelable(!isFirstBooking);

        timePicker.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                timePicker.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!timePicker.isValidTime(timePicker.selectedHour, timePicker.selectedMinute)) {
                            Toast.makeText(context, "Please choose a time between 7:30 AM and 5:00 PM", Toast.LENGTH_SHORT).show();
                        } else {
                            calendar.set(Calendar.HOUR_OF_DAY, timePicker.selectedHour);
                            calendar.set(Calendar.MINUTE, timePicker.selectedMinute);
                            timePicker.dismiss();
                            if (modify) {
                                updateTimeSelectionView();
                            } else {
                                summaryRequest(chained);
                            }
                        }
                    }
                });
                if (isFirstBooking){
                    timePicker.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            new AnimatedDialogBuilder(context)
                                    .setAnimation(AnimatedDialogBuilder.ANIMATION_GROW)
                                    .setTitle(context.getString(R.string.first_service_booking_cancel_title))
                                    .setMessage(context.getString(R.string.first_service_booking_cancel_message))
                                    .setNegativeButton("Continue booking", null) // Do nothing on continue
                                    .setPositiveButton("Quit booking", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                            timePicker.cancel();
                                        }
                                    })
                                    .show();
                        }
                    });
                }
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

        final View customTitle = mLayoutInflater.inflate(R.layout.dialog_custom_title_primary_dark, null, false);
        ((TextView) customTitle.findViewById(R.id.custom_title_text)).setText(context.getString(R.string.service_request_dialog_time_picker_title));
        timePicker.setCustomTitle(customTitle);

        timePicker.show();
    }

    /**
     * Ask for user's additional comments after getting the datetime
     * Also summary the request (date, time, comments) making it intuitive
     * After user confirms, this request will be sent
     */
    private void summaryRequest(final boolean chained) {

        final AnimatedDialogBuilder summaryDialogBuilder = new AnimatedDialogBuilder(context);
        final View view = (mLayoutInflater).inflate(R.layout.dialog_request_service_master, null);
        final TextInputEditText commentEditText = (TextInputEditText) view.findViewById(R.id.dialog_service_request_additional_comments);
        commentEditText.setHint(isFirstBooking ? R.string.service_request_dialog_additional_comments_hint_salesperson
                : R.string.service_request_dialog_additional_comments_hint_comments);

        dateText = (TextView) view.findViewById(R.id.dialog_service_request_date_selection);
        timeText = (TextView) view.findViewById(R.id.dialog_service_request_time_selection);

        updateDateSelectionView();
        updateTimeSelectionView();

        //Provide means to modify the date
        view.findViewById(R.id.dialog_service_request_date_card)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        askForDate(true, false);
                    }
                });

        //Provide means to modify the time
        view.findViewById(R.id.dialog_service_request_time_card)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        askForTime(true, false);
                    }
                });

        // Setup custom title
        final View customTitle = mLayoutInflater.inflate(R.layout.dialog_custom_title_primary_dark, null, false);
        ((TextView) customTitle.findViewById(R.id.custom_title_text)).setText(context.getString(R.string.service_request_dialog_summary_title));

        //Create the summary dialog
        summaryDialogBuilder.setView(view)
                .setCustomTitle(customTitle)
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
                }).setNegativeButton("CANCEL", null);

        final AlertDialog summaryDialog = summaryDialogBuilder.create();

        summaryDialog.setCancelable(!isFirstBooking);
        summaryDialog.setCanceledOnTouchOutside(!isFirstBooking);

        summaryDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                if (isFirstBooking) {
                    summaryDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            new AnimatedDialogBuilder(context)
                                    .setAnimation(AnimatedDialogBuilder.ANIMATION_GROW)
                                    .setTitle(context.getString(R.string.first_service_booking_cancel_title))
                                    .setMessage(context.getString(R.string.first_service_booking_cancel_message))
                                    .setNegativeButton("Continue booking", null) // Do nothing on continue
                                    .setPositiveButton("Quit booking", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.cancel();
                                            summaryDialog.cancel();
                                        }
                                    })
                                    .show();
                        }
                    });
                } else {
                    summaryDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            try {
                                mixpanelHelper.trackButtonTapped("Cancel Request Service", VIEW);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            Toast.makeText(context, "Service Request Cancelled", Toast.LENGTH_SHORT).show();
                            summaryDialog.dismiss();
                        }
                    });
                }
            }
        });

        if (!chained) {
            final View promptTitle = mLayoutInflater.inflate(R.layout.dialog_custom_title_primary_dark, null);
            ((TextView) promptTitle.findViewById(R.id.custom_title_text)).setText(R.string.add_preset_issue_dialog_title);
            new AnimatedDialogBuilder(context)
                    .setCancelable(false)
                    .setCustomTitle(promptTitle)
                    .setMessage(context.getString(R.string.add_preset_issue_dialog_prompt_message))
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startAddingPresetIssues(true);
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            summaryDialog.show();
                        }
                    })
                    .show();
        } else {
            summaryDialog.show();
        }

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

    private void sendRequestWithState(final String state, final String timestamp, final String comments) {
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
                                properties.put(isFirstBooking ? "Salesperson" : "Comments", comments);
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
                            if (isFirstBooking) {
                                ((MainActivity) context).refreshFromServer();
                                ((MainActivity) context).removeTutorial();
                            }
                        } else {
                            Log.e(TAG, "service request: " + requestError.getMessage());
                            Toast.makeText(context, "There was an error, please try again", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

    }

    private class LimitedTimePicker extends TimePickerDialog {

        static final int MAX_HOUR = 17;
        static final int MAX_MINUTE = 0;
        static final int MAX_TIME = MAX_HOUR * 60 + MAX_MINUTE;

        static final int MIN_HOUR = 7;
        static final int MIN_MINUTE = 30;
        static final int MIN_TIME = MIN_HOUR * 60 + MIN_MINUTE;

        int selectedHour = 7;
        int selectedMinute = 30;

        LimitedTimePicker(Context context, OnTimeSetListener listener,
                          int hourOfDay, int minute, boolean is24HourView) {
            super(context, listener, hourOfDay, minute, is24HourView);
            try{
                this.getWindow().setWindowAnimations(R.style.DialogAnimations_slide);
            } catch (NullPointerException e){
                e.printStackTrace();
            }
        }

        @Override
        public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
            super.onTimeChanged(view, hourOfDay, minute);

            selectedHour = hourOfDay;
            selectedMinute = minute;
        }

        /**
         * @param hour selected hour
         * @param minute selected minute
         * @return true if valid
         */
        boolean isValidTime(int hour, int minute){
            int time = hour * 60 + minute;
            Log.d(TAG, "Selected time: " + time);
            Log.d(TAG, "Max: " + MAX_TIME);
            Log.d(TAG, "Min: " + MIN_TIME);
            return time >= MIN_TIME && time <= MAX_TIME;
        }
    }

    private class LimitedDatePicker extends DatePickerDialog {
        int selectedYear = 0;
        int selectedMonth = 0;
        int selectedDay = 0;

        LimitedDatePicker(int day, int month, int year) {
            super(context, null, year, month, year);
            selectedYear = year;
            selectedMonth = month;
            selectedDay = day;
            try{
                this.getWindow().setWindowAnimations(R.style.DialogAnimations_slide);
            } catch (NullPointerException e){
                e.printStackTrace();
            }
        }

        @Override
        public void onDateChanged(DatePicker view, int year, int month, int day) {
            super.onDateChanged(view, year, month, day);
            selectedDay = day;
            selectedMonth = month;
            selectedYear = year;
        }

    }

    /**
     * Prompt user to request preset issues
     * @param chained True if this method is invoked in Booking Service Appointment process,
     *                otherwise false;
     */
    public void startAddingPresetIssues(final boolean chained) {
        showAvailablePresetIssues(chained);
    }

    /**
     * Show all available preset issues
     */
    private void showAvailablePresetIssues(final boolean chained) {
        if (dashboardCar == null) return;

        try {
            mixpanelHelper.trackButtonTapped(MixpanelHelper.ADD_PRESET_ISSUE_BUTTON, MixpanelHelper.DASHBOARD_VIEW);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        View dialogList = mLayoutInflater.inflate(R.layout.dialog_add_preset_issue_list, null);
        final View dialogTitle = mLayoutInflater.inflate(R.layout.dialog_custom_title_primary_dark, null);
        ((TextView) dialogTitle.findViewById(R.id.custom_title_text))
                .setText(context.getString(R.string.add_preset_issue_dialog_title));
        RecyclerView list = (RecyclerView) dialogList.findViewById(R.id.dialog_add_preset_issue_recycler_view);

        final IssueAdapter adapter = new IssueAdapter();
        list.setAdapter(adapter);

        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        list.setLayoutManager(linearLayoutManager);
        list.setHasFixedSize(true);

        final AlertDialog requestIssueDialog = new AnimatedDialogBuilder(context)
                .setAnimation(chained ? AnimatedDialogBuilder.ANIMATION_SLIDE_RIGHT_TO_LEFT : AnimatedDialogBuilder.ANIMATION_GROW)
                .setCustomTitle(dialogTitle)
                .setView(dialogList)
                .setPositiveButton("CONFIRM", null)
                .setNegativeButton("CANCEL", null)
                .create();

        final View serviceDialogTitle = mLayoutInflater.inflate(R.layout.dialog_custom_title_primary_dark, null);
        ((TextView) serviceDialogTitle.findViewById(R.id.custom_title_text))
                .setText(context.getString(R.string.service_request_dialog_prompt_title));

        requestIssueDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button positiveButton = requestIssueDialog.getButton(DialogInterface.BUTTON_POSITIVE);
                positiveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            mixpanelHelper.trackButtonTapped(MixpanelHelper.ADD_PRESET_ISSUE_CONFIRM, MixpanelHelper.DASHBOARD_VIEW);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        List<CarIssuePreset> pickedIssues = adapter.getPickedIssues();
                        if (pickedIssues.size() == 0) {
                            Toast.makeText(context, "Please pick issues you want to add!", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        ((MainActivity) context).showLoading("Saving issue");
                        networkHelper.postMultiplePresetIssue(dashboardCar.getId(), pickedIssues, new RequestCallback() {
                            @Override
                            public void done(String response, RequestError requestError) {
                                ((MainActivity) context).hideLoading();
                                if (requestError == null) {
                                    Log.d(TAG, "Success!");
                                    ((MainActivity) context).showSimpleMessage("We have saved issues you requested!", true);
                                    ((MainActivity) context).refreshFromServer(); // Test this
                                    // Show dialog asking if the user want to book service appointment
                                    if (!chained) {
                                        new AnimatedDialogBuilder(context)
                                                .setCustomTitle(serviceDialogTitle)
                                                .setMessage(context.getString(R.string.service_request_dialog_prompt_message))
                                                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        startBookingService(true);
                                                    }
                                                })
                                                .setNegativeButton("No", null)
                                                .show();
                                    } else {
                                        summaryRequest(true);
                                    }
                                } else {
                                    Log.d(TAG, "Post custom issue failed, error message: " + requestError.getMessage() + ", " +
                                            "error: " + requestError.getError());
                                    ((MainActivity) context).showSimpleMessage("Network error, please try again later.", false);
                                }
                            }
                        });
                        requestIssueDialog.dismiss();
                    }
                });
                Button negativeButton = requestIssueDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
                negativeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (chained){
                            summaryRequest(true);
                        }
                        requestIssueDialog.dismiss();
                    }
                });
            }
        });

        requestIssueDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                try {
                    mixpanelHelper.trackButtonTapped(MixpanelHelper.ADD_PRESET_ISSUE_CANCEL, MixpanelHelper.DASHBOARD_VIEW);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        requestIssueDialog.show();
    }

    /**
     * @param data Chosen preset issue
     */
    private void showDetailDialog(CarIssuePreset data) {
        if (data == null) return;

        View dialogDetail = mLayoutInflater.inflate(R.layout.dialog_add_preset_issue_detail, null);
        final View detailTitle = mLayoutInflater.inflate(R.layout.dialog_custom_title_primary_dark, null);
        ((TextView) detailTitle.findViewById(R.id.custom_title_text))
                .setText(context.getString(R.string.add_preset_issue_dialog_detail_title));

        String title = data.getAction() + " " + data.getItem();
        String description = data.getDescription();
        int severity = data.getPriority();

        ((TextView) dialogDetail.findViewById(R.id.dialog_preset_issue_title_text)).setText(title);
        ((TextView) dialogDetail.findViewById(R.id.dialog_preset_issue_description)).setText(description);

        RelativeLayout rLayout = (RelativeLayout) dialogDetail.findViewById(R.id.dialog_preset_issue_severity_indicator_layout);
        TextView severityTextView = (TextView) dialogDetail.findViewById(R.id.dialog_preset_issue_severity_text);

        switch (severity) {
            case 1:
                rLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.severity_low_indicator));
                severityTextView.setText(context.getResources().getStringArray(R.array.severity_indicators)[0]);
                break;
            case 2:
                rLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.severity_medium_indicator));
                severityTextView.setText(context.getResources().getStringArray(R.array.severity_indicators)[1]);
                break;
            case 3:
                rLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.severity_high_indicator));
                severityTextView.setText(context.getResources().getStringArray(R.array.severity_indicators)[2]);
                break;
            default:
                rLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.severity_critical_indicator));
                severityTextView.setText(context.getResources().getStringArray(R.array.severity_indicators)[3]);
                break;
        }

        final AlertDialog d = new AnimatedDialogBuilder(context)
                .setAnimation(AnimatedDialogBuilder.ANIMATION_GROW)
                .setCustomTitle(detailTitle)
                .setView(dialogDetail)
                .setPositiveButton("OK", null)
                .create();

        d.show();
    }


    public class IssueAdapter extends RecyclerView.Adapter<IssueAdapter.IssueViewHolder> {

        private List<CarIssuePreset> mPresetIssues;

        private List<CarIssuePreset> mPickedIssues;

        private void populateContent() {
            mPresetIssues = new ArrayList<>();
            mPresetIssues.add(new CarIssuePreset.Builder()
                    .setId(4)
                    .setAction(context.getString(R.string.preset_issue_service_emergency))
                    .setItem(context.getString(R.string.preset_issue_item_tow_truck))
                    .setType(CarIssuePreset.TYPE_PRESET)
                    .setDescription(context.getString(R.string.tow_truck_description))
                    .setPriority(5).build());
            mPresetIssues.add(new CarIssuePreset.Builder()
                    .setId(1)
                    .setAction(context.getString(R.string.preset_issue_service_emergency))
                    .setItem(context.getString(R.string.preset_issue_item_flat_tire))
                    .setType(CarIssuePreset.TYPE_PRESET)
                    .setDescription(context.getString(R.string.flat_tire_description))
                    .setPriority(5).build());
            mPresetIssues.add(new CarIssuePreset.Builder()
                    .setId(2)
                    .setAction(context.getString(R.string.preset_issue_service_replace))
                    .setItem(context.getString(R.string.preset_issue_item_engine_oil_filter))
                    .setType(CarIssuePreset.TYPE_PRESET)
                    .setDescription(context.getString(R.string.engine_oil_filter_description))
                    .setPriority(3).build());
            mPresetIssues.add(new CarIssuePreset.Builder()
                    .setId(3)
                    .setAction(context.getString(R.string.preset_issue_service_replace))
                    .setItem(context.getString(R.string.preset_issue_item_wipers_fluids))
                    .setType(CarIssuePreset.TYPE_PRESET)
                    .setDescription(context.getString(R.string.wipers_fluids_description))
                    .setPriority(2).build());
            mPresetIssues.add(new CarIssuePreset.Builder()
                    .setId(5)
                    .setAction(context.getString(R.string.preset_issue_service_request))
                    .setItem(context.getString(R.string.preset_issue_item_shuttle_service))
                    .setType(CarIssuePreset.TYPE_PRESET)
                    .setDescription(context.getString(R.string.shuttle_service_description))
                    .setPriority(3).build());
        }

        public IssueAdapter() {
            populateContent();
            mPickedIssues = new ArrayList<>();
        }

        @Override
        public IssueViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.activity_add_preset_issue_item, parent, false);
            return new IssueViewHolder(v);
        }

        @Override
        public void onBindViewHolder(final IssueViewHolder holder, final int position) {
            final CarIssuePreset presetIssue = mPresetIssues.get(position);

            holder.description.setText(presetIssue.getDescription());
            holder.description.setEllipsize(TextUtils.TruncateAt.END);
            holder.title.setText(String.format("%s %s", presetIssue.getAction(), presetIssue.getItem()));

            holder.container.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        showDetailDialog(presetIssue);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    try {
                        mixpanelHelper.trackButtonTapped("Detail: " + presetIssue.getAction() + " " + presetIssue.getItem(),
                                MixpanelHelper.DASHBOARD_VIEW);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });

            holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        mPickedIssues.add(presetIssue);
                    } else if (mPickedIssues.contains(presetIssue)) {
                        mPickedIssues.remove(presetIssue);
                    }

                    try {
                        String check = isChecked ? "Checked: " : "Unchecked: ";
                        mixpanelHelper.trackButtonTapped(check + presetIssue.getAction() + " " + presetIssue.getItem(),
                                MixpanelHelper.DASHBOARD_VIEW);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            });

            switch (presetIssue.getId()) {
                case 1:
                    holder.imageView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_flat_tire_severe));
                    break;
                case 2:
                    holder.imageView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_replace_orange_48px));
                    break;
                case 3:
                    holder.imageView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_replace_yellow_48px));
                    break;
                case 4:
                    holder.imageView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_tow_truck_severe));
                    break;
                case 5:
                    holder.imageView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.preset_service_medium));
                    break;
                default:
                    holder.imageView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.preset_service_medium));
                    break;
            }
        }

        @Override
        public int getItemCount() {
            return mPresetIssues.size();
        }

        public List<CarIssuePreset> getPickedIssues() {
            return mPickedIssues;
        }

        public class IssueViewHolder extends RecyclerView.ViewHolder {
            public TextView title;
            public TextView description;
            public ImageView imageView;
            public CheckBox checkBox;
            public View container;

            public IssueViewHolder(View itemView) {
                super(itemView);
                checkBox = (CheckBox) itemView.findViewById(R.id.dialog_preset_issue_list_checkbox);
                title = (TextView) itemView.findViewById(R.id.title);
                description = (TextView) itemView.findViewById(R.id.description);
                imageView = (ImageView) itemView.findViewById(R.id.image_icon);
                container = itemView.findViewById(R.id.list_car_item);
            }
        }
    }
}
