package com.pitstop.ui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.pitstop.R;
import com.pitstop.database.LocalCarIssueAdapter;
import com.pitstop.models.Car;
import com.pitstop.models.CarIssue;
import com.pitstop.models.CarIssuePreset;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.utils.NetworkHelper;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import static com.pitstop.R.drawable.severity_critical_indicator_2;
import static com.pitstop.R.drawable.severity_high_indicator;
import static com.pitstop.R.drawable.severity_low_indicator;
import static com.pitstop.R.drawable.severity_medium_indicator;

/**
 * Created by yifan on 16/9/23.
 */
public class AddPresetIssueActivity extends AppCompatActivity {

    private static final String TAG = AddPresetIssueActivity.class.getSimpleName();

    private static final String SERVICE_EMERGENCY = "Emergency";
    private static final String SERVICE_REPLACE = "Replace";
    private static final String ITEM_FLAT_TIRE = "Flat Tire";
    private static final String ITEM_TOW_TRUCK = "Tow Truck";
    private static final String ITEM_ENGINE_OIL_FILTER = "Engine Oil & Filter";
    private static final String ITEM_WIPERS_FLUIDS = "Wipers/Fluids";

    public static final String EXTRA_CAR = "car";

    private List<CarIssuePreset> mAvailableIssueList;

    private List<String> mTypes = new ArrayList<>();
    private List<Integer> mIds = new ArrayList<>();
    private List<String> mItems = new ArrayList<>();
    private List<String> mActions = new ArrayList<>();
    private List<String> mDescriptions = new ArrayList<>();

    private Car mCar;

    private String mPickedAction;
    private String mPickedItem;
    private String mDescription = "";
    private int mPriority;

    private View rootView;
    private TextView mAction;
    private TextView mItem;
    private CardView mActionButton;
    private CardView mItemButton;
    private RelativeLayout mDescriptionContainer;

    private LocalCarIssueAdapter mCarIssueAdapter;
    private NetworkHelper mNetworkHelper;

    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rootView = getLayoutInflater().inflate(R.layout.activity_add_preset_issue, null);
        setContentView(rootView);

        // Get extra data
        mCar = getIntent().getParcelableExtra(EXTRA_CAR);

        overridePendingTransition(R.anim.activity_bottom_up_in, R.anim.activity_bottom_up_out);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setupProgressDialog();
        setupUI();

        mCarIssueAdapter = new LocalCarIssueAdapter(this);
        mNetworkHelper = new NetworkHelper(this);

        retrieveAvailableServices();

    }

    private void setupUI() {
        ((TextView) findViewById(R.id.car_name)).setText(mCar.getYear() + " " + mCar.getMake() + " " + mCar.getModel());
        ((TextView) findViewById(R.id.dealership_name)).setText(mCar.getDealership().getName());
        mAction = (TextView) findViewById(R.id.custom_action);
        mItem = (TextView) findViewById(R.id.custom_item);
        mActionButton = (CardView) findViewById(R.id.custom_issue_action_button);
        mItemButton = (CardView) findViewById(R.id.custom_issue_item_button);
        mDescriptionContainer = (RelativeLayout) findViewById(R.id.custom_issue_description_container);
        mDescriptionContainer.setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();


    }

    @Override
    public void onBackPressed() {
        if (mPickedAction != null && mPickedItem != null) {
            showSaveDialog();
            return;
        }
        super.onBackPressed();
        overridePendingTransition(R.anim.activity_bottom_down_in, R.anim.activity_bottom_down_out);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_add_custom_issue, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.custom_issue_save:
                // TODO: 16/9/28 Post to backend
                if (mPickedAction != null && mPickedItem != null) {
                    postCarIssueToBackend();
                } else {
                    Snackbar.make(rootView, "Please select the issue you want!", Snackbar.LENGTH_SHORT).show();
                }
                break;
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Onclick method for picking issue action
     *
     * @param view
     */
    public void pickAction(View view) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(AddPresetIssueActivity.this)
                .setTitle("Please pick the service category")
                .setItems(getResources().getStringArray(R.array.preset_issue_actions),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mPickedAction = getResources().getStringArray(R.array.preset_issue_actions)[which];
                                mAction.setText(mPickedAction);
                                pickItem(null);
                            }
                        })
                .setCancelable(false);
        dialog.show();
    }

    /**
     * Onclick method for picking issue item
     *
     * @param view
     */
    public void pickItem(View view) {
        if (mPickedAction == null) {
            Snackbar.make(rootView, "Please pick your service category first", Snackbar.LENGTH_SHORT).show();
            pickAction(null);
            return;
        }
        final String[] items = mPickedAction.equals(SERVICE_EMERGENCY) ? getResources().getStringArray(R.array.emergency_item)
                : getResources().getStringArray(R.array.replace_item);
        AlertDialog.Builder dialog = new AlertDialog.Builder(AddPresetIssueActivity.this)
                .setTitle("Please pick the service item")
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mPickedItem = items[which];
                        mItem.setText(mPickedItem);

                        // Hard coded value for the time being, if we decide to make more preset issues
                        // then this part has to be modified to keep the code easily maintainable
                        if (mPickedItem.equals(ITEM_FLAT_TIRE)) {
                            mDescription = getString(R.string.flat_tire_description);
                            mPriority = 5;
                        } else if (mPickedItem.equals(ITEM_TOW_TRUCK)) {
                            mDescription = getString(R.string.tow_truck_description);
                            mPriority = 5;
                        } else if (mPickedItem.equals(ITEM_ENGINE_OIL_FILTER)) {
                            mDescription = getString(R.string.engine_oil_filter_description);
                            mPriority = 3;
                        } else if (mPickedItem.equals(ITEM_WIPERS_FLUIDS)) {
                            mDescription = getString(R.string.wipers_fluids_description);
                            mPriority = 2;
                        }

                        setupDescriptionView();
                    }
                })
                .setCancelable(false);
        dialog.show();
    }

    private void retrieveAvailableServices() {
        showLoading("Getting available services");
        mNetworkHelper.getCustomServices(mCar.getId(), new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                hideLoading();
                if (requestError != null) {
                    showErrorAndFinish();
                } else {
                    try {

                        mAvailableIssueList = CarIssuePreset.createCustomCarIssues(response);

                        for (CarIssuePreset presetIssue : mAvailableIssueList) {
                            mTypes.add(presetIssue.getType());
                            mIds.add(presetIssue.getId());
                            mItems.add(presetIssue.getItem());
                            mActions.add(presetIssue.getAction());
                            mDescriptions.add(presetIssue.getDescription());
                        }

                        Snackbar.make(rootView, "Retrieved all available issues!", Snackbar.LENGTH_SHORT).show();


                    } catch (JSONException e) {
                        e.printStackTrace();
                        showErrorAndFinish();
                    }
                }
            }
        });
    }

    private void showSaveDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(AddPresetIssueActivity.this)
                .setTitle("Save custom issue")
                .setMessage("You have not saved your custom issue yet! Are you sure you want to quit?")
                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        postCarIssueToBackend();
                    }
                })
                .setNegativeButton("CANCEL", null);
        dialog.show();
    }

    /**
     * After the user has selected the desired issue, we show them detail information about the issue
     */
    private void setupDescriptionView() {

        RelativeLayout severityIndicatorLayout = (RelativeLayout) rootView.findViewById(R.id.custom_severity_indicator_layout);
        TextView severityTextView = (TextView) rootView.findViewById(R.id.custom_issue_severity_text);

        String title = mPickedAction + " " + mPickedItem;
        ((TextView) findViewById(R.id.custom_issue_title_text)).setText(title);
        ((TextView) findViewById(R.id.custom_issue_description)).setText(mDescription);

        switch (mPriority) {
            case 1:
                severityIndicatorLayout.setBackground(ContextCompat.getDrawable(this, severity_low_indicator));
                severityTextView.setText(getResources().getStringArray(R.array.severity_indicators)[0]);
                break;
            case 2:
                severityIndicatorLayout.setBackground(ContextCompat.getDrawable(this, severity_medium_indicator));
                severityTextView.setText(getResources().getStringArray(R.array.severity_indicators)[1]);
                break;
            case 3:
                severityIndicatorLayout.setBackground(ContextCompat.getDrawable(this, severity_high_indicator));
                severityTextView.setText(getResources().getStringArray(R.array.severity_indicators)[2]);
                break;
            default:
                severityIndicatorLayout.setBackground(ContextCompat.getDrawable(this, severity_critical_indicator_2));
                severityTextView.setText(getResources().getStringArray(R.array.severity_indicators)[3]);
                break;
        }

        mDescriptionContainer.setVisibility(View.VISIBLE);
    }

    /**
     * do POST to car issue, call finish() on success, do nothing on error
     */
    private void postCarIssueToBackend() {
        if (mPickedAction == null || mPickedItem == null) {
            Snackbar.make(rootView, "You didn't pick the service yet!", Snackbar.LENGTH_SHORT).show();
            return;
        }

        showLoading("Saving issue");

        // TODO: 16/9/28 Save issues


    }

    /**
     * Onclick method for "Request service right now" button<br>
     * Post new issue and then let the go back to MainActivity and request service
     *
     * @param view Button
     */
    public void postIssueAndRequestService(View view) {

    }

    private void setupProgressDialog() {
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setCanceledOnTouchOutside(false);
    }

    public void hideLoading() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        } else {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setCanceledOnTouchOutside(false);
        }
    }

    public void showLoading(String text) {
        if (mProgressDialog == null) {
            return;
        }
        mProgressDialog.setMessage(text);
        if (!mProgressDialog.isShowing()) {
            mProgressDialog.show();
        }
    }

    private void showErrorAndFinish() {
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage("We have problem retrieving data. We apologize for the inconvenience, please try again later.")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finishOnFailure();
                    }
                })
                .setCancelable(false)
                .show();
    }

    private void finishOnSuccess() {
        setResult(RESULT_OK);
        super.onBackPressed();
        overridePendingTransition(R.anim.activity_bottom_down_in, R.anim.activity_bottom_down_out);
        finish();
    }

    private void finishOnFailure() {
        setResult(RESULT_CANCELED);
        super.onBackPressed();
        overridePendingTransition(R.anim.activity_bottom_down_in, R.anim.activity_bottom_down_out);
        finish();
    }

}
