package com.pitstop.ui.service_request;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.pitstop.EventBus.CarDataChangedEvent;
import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.EventBus.EventType;
import com.pitstop.EventBus.EventTypeImpl;
import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerTempNetworkComponent;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.TempNetworkComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.get.GetUserCarUseCase;
import com.pitstop.models.Car;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.ui.ILoadingActivity;
import com.pitstop.ui.main_activity.MainActivity;
import com.pitstop.ui.service_request.view_fragment.AddCustomIssueDialog;
import com.pitstop.ui.service_request.view_fragment.LimitedDatePickerDialog;
import com.pitstop.ui.service_request.view_fragment.LimitedTimePickerDialog;
import com.pitstop.ui.service_request.view_fragment.ServiceIssueAdapter;
import com.pitstop.utils.AnimatedDialogBuilder;
import com.pitstop.utils.DateTimeFormatUtil;
import com.pitstop.utils.MixpanelHelper;
import com.pitstop.utils.NetworkHelper;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import io.smooch.core.Smooch;

/**
 * Created by yifan on 16/11/24.
 */

public class ServiceRequestActivity extends AppCompatActivity
        implements DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener,
        AddCustomIssueDialog.CustomIssueCallback, ILoadingActivity {

    private static final String TAG = ServiceRequestActivity.class.getSimpleName();
    public static final String EXTRA_CAR = "extra_car";
    public static final String EXTRA_FIRST_BOOKING = "is_first_booking";
    public static final String STATE_TENTATIVE = "tentative";
    public static final String STATE_REQUESTED = "requested";

    public static final EventSource EVENT_SOURCE
            = new EventSourceImpl(EventSource.SOURCE_REQUEST_SERVICE);

    private GlobalApplication application;
    private MixpanelHelper mixpanelHelper;
    private NetworkHelper networkHelper;

    private Car dashboardCar;
    private Calendar mCalendar;
    private boolean isFirstBooking;
    private String timestamp;
    private String comments;
    private List<CarIssue> pickedCustomIssues = new ArrayList<>();

    private TextView mDate;
    private TextView mTime;
    private TextView mDealership;
    private TextInputEditText mComments;
    private RecyclerView mIssueList;
    private ServiceIssueAdapter mIssueAdapter;
    private ProgressDialog progressDialog;

    private boolean shouldRefresh = false;
    private boolean shouldRemoveTutorial = false;

    private UseCaseComponent useCaseComponent;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_service);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close_24dp);

        TempNetworkComponent tempNetworkComponent = DaggerTempNetworkComponent.builder()
                .contextModule(new ContextModule(this))
                .build();

        application = (GlobalApplication) getApplicationContext();
        networkHelper = tempNetworkComponent.networkHelper();
        mixpanelHelper = new MixpanelHelper(application);

        isFirstBooking = getIntent().getExtras().getBoolean(EXTRA_FIRST_BOOKING);
        mCalendar = Calendar.getInstance();

        useCaseComponent = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(getApplicationContext()))
                .build();

        setupStaticUI();
        showLoading(getString(R.string.loading));

        useCaseComponent.getUserCarUseCase().execute(new GetUserCarUseCase.Callback() {
            @Override
            public void onCarRetrieved(Car car) {
                dashboardCar = car;
                populateUI();
                hideLoading(null);
            }

            @Override
            public void onNoCarSet() {

            }

            @Override
            public void onError() {
            }
        });

    }

    private void setupStaticUI(){
        mDate = (TextView) findViewById(R.id.activity_service_request_date_selection);
        mTime = (TextView) findViewById(R.id.activity_service_request_time_selection);
        mDealership = (TextView) findViewById(R.id.activity_service_request_shop_location);
        mIssueList = (RecyclerView) findViewById(R.id.activity_service_request_issue_list);
        mComments = (TextInputEditText) findViewById(R.id.activity_service_request_comments);

        setupCalendar();

        progressDialog = new ProgressDialog(this);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(false);
    }

    private void populateUI() {

        mDealership.setText(dashboardCar.getDealership().getName());
        if (isFirstBooking) mComments.setHint("The name of your salesperson");

        mIssueAdapter = new ServiceIssueAdapter(this, dashboardCar.getActiveIssues());
        mIssueList.setAdapter(mIssueAdapter);
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mIssueList.setLayoutManager(linearLayoutManager);
    }

    private void setupCalendar() {
        mCalendar.set(Calendar.HOUR_OF_DAY, LimitedTimePickerDialog.MIN_HOUR);
        mCalendar.set(Calendar.MINUTE, LimitedTimePickerDialog.MIN_MINUTE);
        mCalendar.add(Calendar.DAY_OF_MONTH, isFirstBooking ? 90 : 1);
        onDateSet(null, mCalendar.get(Calendar.YEAR), mCalendar.get(Calendar.MONTH), mCalendar.get(Calendar.DAY_OF_MONTH));
        onTimeSet(null, 7, 30);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_request_service, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (dashboardCar == null){
            Toast.makeText(application,"Loading information, please wait",Toast.LENGTH_LONG);
            return false;
        }

        switch (id) {
            case R.id.send:
                if(dashboardCar.getDealership().getEmail().equals("") && dashboardCar.getDealership().isCustom()){
                    Toast.makeText(getApplicationContext(),"Please add an email to this shop",Toast.LENGTH_SHORT).show();
                }
                else if (pickedCustomIssues != null && !pickedCustomIssues.isEmpty()) {
                    showLoading("Saving issue");
                    networkHelper.postMultiplePresetIssue(dashboardCar.getId(), pickedCustomIssues, new RequestCallback() {
                        @Override
                        public void done(String response, RequestError requestError) {
                            if (requestError == null) {
                                hideLoading("Success!");
                                showSimpleMessage("We have saved issues you requested!", true);
                                shouldRefresh = true;

                                //Notify change
                                EventType type = new EventTypeImpl(EventType.EVENT_SERVICES_NEW);
                                EventBus.getDefault()
                                        .post(new CarDataChangedEvent(type,EVENT_SOURCE));
                            } else {
                                hideLoading("Failed!");
                                showSimpleMessage("Network error, please try again later.", false);
                            }
                            //format the timestamp before sending the network request because the server use ISO8601 format
                            timestamp = DateTimeFormatUtil.formatToISO8601(mCalendar);
                            comments = mComments.getText().toString();
                            if (isFirstBooking) {
                                sendRequestWithState(STATE_TENTATIVE, timestamp, comments);
                            } else {
                                sendRequestWithState(STATE_REQUESTED, timestamp, comments);
                            }
                        }
                    });
                } else {
                    timestamp = DateTimeFormatUtil.formatToISO8601(mCalendar);
                    comments = mComments.getText().toString();
                    if (isFirstBooking) {
                        sendRequestWithState(STATE_TENTATIVE, timestamp, comments);
                    } else {
                        sendRequestWithState(STATE_REQUESTED, timestamp, comments);
                    }
                }
                break;
            case android.R.id.home:
                if (isFirstBooking) {
                    new AnimatedDialogBuilder(ServiceRequestActivity.this)
                            .setAnimation(AnimatedDialogBuilder.ANIMATION_GROW)
                            .setTitle(getString(R.string.first_service_booking_cancel_title))
                            .setMessage(getString(R.string.first_service_booking_cancel_message))
                            .setNegativeButton("Continue booking", null) // Do nothing on continue
                            .setPositiveButton("Quit booking", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent intent = new Intent();
                                    intent.putExtra(MainActivity.REFRESH_FROM_SERVER, shouldRefresh);
                                    intent.putExtra(MainActivity.REMOVE_TUTORIAL_EXTRA, false);
                                    setResult(RESULT_CANCELED, intent);
                                    finish();
                                }
                            })
                            .show();
                } else {
                    Intent intent = new Intent();
                    intent.putExtra(MainActivity.REFRESH_FROM_SERVER, shouldRefresh);
                    intent.putExtra(MainActivity.REMOVE_TUTORIAL_EXTRA, false);
                    setResult(RESULT_CANCELED, intent);
                    finish();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (isFirstBooking) {
            new AnimatedDialogBuilder(ServiceRequestActivity.this)
                    .setAnimation(AnimatedDialogBuilder.ANIMATION_GROW)
                    .setTitle(getString(R.string.first_service_booking_cancel_title))
                    .setMessage(getString(R.string.first_service_booking_cancel_message))
                    .setNegativeButton("Continue booking", null) // Do nothing on continue
                    .setPositiveButton("Quit booking", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                JSONObject properties = new JSONObject();
                                properties.put("Button", "Cancel Service Request");
                                properties.put("State", "Tentative");
                                properties.put("View", MixpanelHelper.DASHBOARD_VIEW);
                                mixpanelHelper.trackCustom(MixpanelHelper.EVENT_BUTTON_TAPPED, properties);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            Intent intent = new Intent();
                            intent.putExtra(MainActivity.REFRESH_FROM_SERVER, shouldRefresh);
                            intent.putExtra(MainActivity.REMOVE_TUTORIAL_EXTRA, false);
                            setResult(RESULT_CANCELED, intent);
                            ServiceRequestActivity.super.onBackPressed();
                        }
                    })
                    .show();
        } else {
            Intent intent = new Intent();
            intent.putExtra(MainActivity.REFRESH_FROM_SERVER, shouldRefresh);
            intent.putExtra(MainActivity.REMOVE_TUTORIAL_EXTRA, false);
            setResult(RESULT_CANCELED, intent);
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void finish() {
        super.finish();
    }

    /**
     * @param view Date textView
     */
    public void selectDate(View view) {
        final LimitedDatePickerDialog dateDialog = new LimitedDatePickerDialog(this, this,
                mCalendar.get(Calendar.YEAR), mCalendar.get(Calendar.MONTH), mCalendar.get(Calendar.DAY_OF_MONTH));

        dateDialog.setCanceledOnTouchOutside(!isFirstBooking);
        dateDialog.setCancelable(!isFirstBooking);
        dateDialog.setTitle(getString(R.string.service_request_dialog_date_picker_title));

        dateDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(final DialogInterface dialog) {
                dateDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!dateDialog.isValidDate()) {
                            Toast.makeText(ServiceRequestActivity.this, "Please choose a date in the future", Toast.LENGTH_SHORT).show();
                        } else {
                            onDateSet(null, dateDialog.selectedYear, dateDialog.selectedMonth, dateDialog.selectedDay);
                            dialog.dismiss();
                        }
                    }
                });
            }
        });

        dateDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                mixpanelHelper.trackButtonTapped("Cancel Request Service", MixpanelHelper.SERVICE_REQUEST_VIEW);
            }
        });

        dateDialog.show();
    }

    /**
     * @param view Time textView
     */
    public void selectTime(View view) {
        final LimitedTimePickerDialog timePicker = new LimitedTimePickerDialog(this, this, mCalendar.get(Calendar.HOUR_OF_DAY),
                mCalendar.get(Calendar.MINUTE), false);

        timePicker.setCanceledOnTouchOutside(!isFirstBooking);
        timePicker.setCancelable(!isFirstBooking);
        timePicker.setTitle(getString(R.string.service_request_dialog_time_picker_title));

        timePicker.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                timePicker.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!timePicker.isValidTime()) {
                            Toast.makeText(ServiceRequestActivity.this, "Please choose a time between 7:30 AM and 5:00 PM", Toast.LENGTH_SHORT).show();
                        } else {
                            onTimeSet(null, timePicker.selectedHour, timePicker.selectedMinute);
                            timePicker.dismiss();
                        }
                    }
                });
            }
        });

        timePicker.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                mixpanelHelper.trackButtonTapped("Cancel Request Service", MixpanelHelper.SERVICE_REQUEST_VIEW);
            }
        });

        timePicker.show();
    }

    /**
     * @param view FAB
     */
    public void addCustomIssues(View view) {
        FragmentManager fm = getSupportFragmentManager();
        AddCustomIssueDialog presetPicker = AddCustomIssueDialog.newInstance(this, pickedCustomIssues);
        presetPicker.setCallback(this);
        presetPicker.show(fm, "Add Custom Service");
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        Log.d(TAG, "On date set");
        mCalendar.set(Calendar.YEAR, year);
        mCalendar.set(Calendar.MONTH, month);
        mCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        if (mDate != null) mDate.setText(DateTimeFormatUtil.formatToReadableDate(mCalendar));
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        Log.d(TAG, "On time set");
        mCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        mCalendar.set(Calendar.MINUTE, minute);
        if (mTime != null) mTime.setText(DateTimeFormatUtil.formatToReadableTime(mCalendar));
    }

    @Override
    public void onCustomIssueSelected(List<CarIssue> selectedIssues) {
        Log.d(TAG, "Picked issue size: " + selectedIssues);
        pickedCustomIssues = selectedIssues;
        mIssueAdapter.setPickedCustomIssues(selectedIssues);
    }

    private void sendRequestWithState(final String state, final String timestamp, final String comments) {
        Log.d("Service Request", "Timestamp: " + timestamp);
        networkHelper.requestService(application.getCurrentUserId(), dashboardCar.getId(), dashboardCar.getShopId(),
                state, timestamp, comments, new RequestCallback() {
                    @Override
                    public void done(String response, RequestError requestError) {
                        if (requestError == null) {
                            shouldRefresh = true;
                            try {
                                JSONObject properties = new JSONObject();
                                properties.put("Button", "Confirm Service Request");
                                properties.put("View", MixpanelHelper.SERVICE_REQUEST_VIEW);
                                properties.put("State", isFirstBooking ? "Tentative" : "Requested"); // changes
                                properties.put(isFirstBooking ? "Salesperson" : "Comments", comments);
                                properties.put("Number of Services Requested", dashboardCar.getActiveIssues().size());
                                mixpanelHelper.trackCustom(MixpanelHelper.EVENT_BUTTON_TAPPED, properties);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            Toast.makeText(application, "Service request sent", Toast.LENGTH_SHORT).show();
                            Smooch.track("User Requested Service");
                            for (CarIssue issue : dashboardCar.getActiveIssues()) {
                                if (issue.getStatus().equals(CarIssue.ISSUE_NEW)) {
                                    networkHelper.setIssuePending(dashboardCar.getId(), issue.getId(), null);
                                }
                            }

                            if (isFirstBooking) {
                                shouldRemoveTutorial = true;
                            }

                            Intent data = new Intent();
                            data.putExtra(MainActivity.REFRESH_FROM_SERVER, shouldRefresh);
                            data.putExtra(MainActivity.REMOVE_TUTORIAL_EXTRA, shouldRemoveTutorial);
                            setResult(RESULT_OK, data);
                            finish();
                        } else {
                            Log.e(TAG, "service request: " + requestError.getMessage());
                            Toast.makeText(application, "There was an error, please try again", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

    }

    /**
     * Create and show an snackbar that is used to show users some information.<br>
     * The purpose of this method is to display message that requires user's confirm to be dismissed.
     *
     * @param content snack bar message
     */
    public void showSimpleMessage(@NonNull String content, boolean isSuccess) {
        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), content, Snackbar.LENGTH_LONG)
                .setAction("OK", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // DO nothing
                    }
                })
                .setActionTextColor(Color.WHITE);
        View snackBarView = snackbar.getView();
        if (isSuccess) {
            snackBarView.setBackgroundColor(ContextCompat.getColor(this, R.color.message_success));
        } else {
            snackBarView.setBackgroundColor(ContextCompat.getColor(this, R.color.message_failure));
        }
        TextView textView = (TextView) snackBarView.findViewById(android.support.design.R.id.snackbar_text);
        textView.setTextColor(ContextCompat.getColor(this, R.color.white_text));

        snackbar.show();
    }

    @Override
    public void hideLoading(String string) {
        if (progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        if (string != null) {
            Toast.makeText(this, string, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void showLoading(String string) {
        progressDialog.setMessage(string);
        if (!progressDialog.isShowing()) {
            progressDialog.show();
        }
    }

    public void checkShopDetail(View view) {

        final View dialogLayout = LayoutInflater.from(this).inflate(R.layout.dialog_dealership_detail, null, false);
        final TextView address = (TextView) dialogLayout.findViewById(R.id.dialog_dealership_address);
        address.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mixpanelHelper.trackButtonTapped("Directions to " + dashboardCar.getDealership().getName(),
                        MixpanelHelper.SERVICE_REQUEST_VIEW);


                String uri = String.format(Locale.ENGLISH, "http://maps.google.com/maps?daddr=%s",
                        dashboardCar.getDealership().getAddress());
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                startActivity(intent);
            }
        });
        final TextView phone = (TextView) dialogLayout.findViewById(R.id.dialog_dealership_phone);
        phone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mixpanelHelper.trackButtonTapped("Confirm call to " + dashboardCar.getDealership().getName(),
                        MixpanelHelper.SERVICE_REQUEST_VIEW);

                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" +
                        dashboardCar.getDealership().getPhone()));
                startActivity(intent);
            }
        });

        address.setText(dashboardCar.getDealership().getAddress());
        phone.setText(dashboardCar.getDealership().getPhone());

        new AnimatedDialogBuilder(this)
                .setAnimation(AnimatedDialogBuilder.ANIMATION_GROW)
                .setTitle(dashboardCar.getDealership().getName())
                .setView(dialogLayout)
                .setPositiveButton("OK", null)
                .show();
    }

}
