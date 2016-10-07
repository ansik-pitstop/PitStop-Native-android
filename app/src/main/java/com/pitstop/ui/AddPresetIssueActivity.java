package com.pitstop.ui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.pitstop.R;
import com.pitstop.models.Car;
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
 * @deprecated For the time being this activity does not need to be used.
 * In the future when we have more preset issues, then we can leverage a new activity instead of a dialog.
 */
public class AddPresetIssueActivity extends AppCompatActivity {

    private static final String TAG = AddPresetIssueActivity.class.getSimpleName();

    public static final String EXTRA_CAR = "car";

    private Car mCar;

//    private String mPickedAction;
//    private String mPickedItem;
//    private String mDescription = "";
//    private int mPriority;

    private View rootView;

    private RecyclerView mRecyclerView;
    private IssueAdapter mIssueAdapter;
    private List<CarIssuePreset> mAvailableIssueList;

    private ProgressDialog mProgressDialog;

    private NetworkHelper mNetworkHelper;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rootView = getLayoutInflater().inflate(R.layout.activity_add_preset_issue, null);
        setContentView(rootView);
        overridePendingTransition(R.anim.activity_bottom_up_in, R.anim.activity_bottom_up_out);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mCar = getIntent().getParcelableExtra(EXTRA_CAR);

        setupProgressDialog();
        setupUI();

        // Get extra data
        mNetworkHelper = new NetworkHelper(getApplicationContext());

        // Check local database content for preset issues;
        // If empty, ask backend for them, if not, load them
    }

    private void setupUI() {
        ((TextView) findViewById(R.id.car_name)).setText(mCar.getYear() + " " + mCar.getMake() + " " + mCar.getModel());
        ((TextView) findViewById(R.id.dealership_name)).setText(mCar.getDealership().getName());
        mRecyclerView = (RecyclerView) findViewById(R.id.custom_issue_list);
        populateListContents();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.activity_bottom_down_in, R.anim.activity_bottom_down_out);
        finish();
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
                break;
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void populateListContents() {
        mAvailableIssueList = new ArrayList<>();
        mAvailableIssueList.add(new CarIssuePreset.Builder()
                .setAction(getString(R.string.preset_issue_service_emergency))
                .setItem(getString(R.string.preset_issue_item_tow_truck))
                .setType(CarIssuePreset.TYPE_USER_INPUT)
                .setDescription(getString(R.string.tow_truck_description))
                .setPriority(5).build());
        mAvailableIssueList.add(new CarIssuePreset.Builder()
                .setAction(getString(R.string.preset_issue_service_emergency))
                .setItem(getString(R.string.preset_issue_item_flat_tire))
                .setType(CarIssuePreset.TYPE_USER_INPUT)
                .setDescription(getString(R.string.flat_tire_description))
                .setPriority(5).build());
        mAvailableIssueList.add(new CarIssuePreset.Builder()
                .setAction(getString(R.string.preset_issue_service_replace))
                .setItem(getString(R.string.preset_issue_item_engine_oil_filter))
                .setType(CarIssuePreset.TYPE_USER_INPUT)
                .setDescription(getString(R.string.engine_oil_filter_description))
                .setPriority(3).build());
        mAvailableIssueList.add(new CarIssuePreset.Builder()
                .setAction(getString(R.string.preset_issue_service_replace))
                .setItem(getString(R.string.preset_issue_item_wipers_fluids))
                .setType(CarIssuePreset.TYPE_USER_INPUT)
                .setDescription(getString(R.string.wipers_fluids_description))
                .setPriority(2).build());
        mAvailableIssueList.add(new CarIssuePreset.Builder()
                .setAction(getString(R.string.preset_issue_service_request))
                .setItem(getString(R.string.preset_issue_item_shuttle_service))
                .setType(CarIssuePreset.TYPE_USER_INPUT)
                .setDescription(getString(R.string.shuttle_service_description))
                .setPriority(3).build());

        mRecyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(linearLayoutManager);
        mIssueAdapter = new IssueAdapter(mAvailableIssueList);
        mRecyclerView.setAdapter(mIssueAdapter);
    }

    /**
     * Not needed for the time being
     */
    private void retrieveAvailableServices() {
        showLoading("Getting available services");
        mNetworkHelper.getPresetIssuesByCarId(mCar.getId(), new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                hideLoading();
                if (requestError != null) {
                    showErrorAndFinish();
                } else {
                    try {
                        mAvailableIssueList = CarIssuePreset.createCustomCarIssues(response);
//                        mLocalPresetIssueStore.storePresetIssues(mAvailableIssueList, mCar.getId());
                        Snackbar.make(rootView, "Retrieved all available issues!", Snackbar.LENGTH_SHORT).show();

                    } catch (JSONException e) {
                        e.printStackTrace();
                        showErrorAndFinish();
                    }
                }
            }
        });
    }


    /**
     * After the user has selected the desired issue, we show them detail information about the issue
     */
    private void setupDescriptionView() {
//        RelativeLayout severityIndicatorLayout = (RelativeLayout) rootView.findViewById(R.id.custom_severity_indicator_layout);
//        TextView severityTextView = (TextView) rootView.findViewById(R.id.custom_issue_severity_text);
//
//        String title = mPickedAction + " " + mPickedItem;
//        ((TextView) findViewById(R.id.custom_issue_title_text)).setText(title);
//        ((TextView) findViewById(R.id.custom_issue_description)).setText(mDescription);
//
//        switch (mPriority) {
//            case 1:
//                severityIndicatorLayout.setBackground(ContextCompat.getDrawable(this, severity_low_indicator));
//                severityTextView.setText(getResources().getStringArray(R.array.severity_indicators)[0]);
//                break;
//            case 2:
//                severityIndicatorLayout.setBackground(ContextCompat.getDrawable(this, severity_medium_indicator));
//                severityTextView.setText(getResources().getStringArray(R.array.severity_indicators)[1]);
//                break;
//            case 3:
//                severityIndicatorLayout.setBackground(ContextCompat.getDrawable(this, severity_high_indicator));
//                severityTextView.setText(getResources().getStringArray(R.array.severity_indicators)[2]);
//                break;
//            default:
//                severityIndicatorLayout.setBackground(ContextCompat.getDrawable(this, severity_critical_indicator_2));
//                severityTextView.setText(getResources().getStringArray(R.array.severity_indicators)[3]);
//                break;
//        }
//
//        mDescriptionContainer.setVisibility(View.VISIBLE);
    }

    /**
     * do POST to car issue, call finish() on success, do nothing on error
     */
    private void postCarIssueToBackend(CarIssuePreset issue) {
        showLoading("Saving issue");
        if (issue.getType().equals(CarIssuePreset.TYPE_USER_INPUT)) {
            mNetworkHelper.postUserInputIssue(mCar.getId(), issue.getItem(), issue.getAction(),
                    issue.getDescription(), issue.getPriority(), new RequestCallback() {
                        @Override
                        public void done(String response, RequestError requestError) {
                            hideLoading();
                            if (requestError == null) {
                                Log.d(TAG, "Success!");
                                finishOnSuccess();
                            } else {
                                Log.d(TAG, "Post custom issue failed, error message: " + requestError.getMessage() + ", " +
                                        "error: " + requestError.getError());
                                showErrorAndFinish();
                            }
                        }
                    });
        }
    }

    /**
     * Show a detail dialog about the issue
     * @param pickedCarIssue
     */
    private void showDetailDialog(final CarIssuePreset pickedCarIssue) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_preset_issue_detail, null);
        View titleView = getLayoutInflater().inflate(R.layout.dialog_add_preset_issue_title, null);
        RelativeLayout severityIndicatorLayout = (RelativeLayout) dialogView.findViewById(R.id.dialog_preset_issue_severity_indicator_layout);
        TextView severityTextView = (TextView) dialogView.findViewById(R.id.dialog_preset_issue_severity_text);
        String title = pickedCarIssue.getAction() + " " + pickedCarIssue.getItem();
        ((TextView) dialogView.findViewById(R.id.dialog_preset_issue_title_text)).setText(title);
        ((TextView) dialogView.findViewById(R.id.dialog_preset_issue_description)).setText(pickedCarIssue.getDescription());

        switch (pickedCarIssue.getPriority()) {
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
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setView(dialogView)
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        postCarIssueToBackend(pickedCarIssue);
                    }
                })
                .setNegativeButton("Cancel", null)
                .setCustomTitle(titleView).show();
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
                .setMessage("We have problem communicating with our server. We apologize for the inconvenience, please try again later.")
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

    private void showErrorMessage() {
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage("We have problem sending your request. We apologize for the inconvenience, please try again later.")
                .setPositiveButton("OK", null)
                .setCancelable(false)
                .show();
    }

    public class IssueAdapter extends RecyclerView.Adapter<IssueAdapter.IssueViewHolder> {

        private List<CarIssuePreset> mPresetIssues;

        public IssueAdapter(List<CarIssuePreset> presetIssues) {
            mPresetIssues = presetIssues;
        }

        @Override
        public IssueViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.activity_add_preset_issue_item, parent, false);
            return new IssueViewHolder(v);
        }

        @Override
        public void onBindViewHolder(IssueViewHolder holder, final int position) {
            final CarIssuePreset presetIssue = mPresetIssues.get(position);

            holder.description.setText(presetIssue.getDescription());
            holder.description.setEllipsize(TextUtils.TruncateAt.END);
            holder.title.setText(String.format("%s %s", presetIssue.getAction(), presetIssue.getItem()));

            holder.container.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showDetailDialog(mPresetIssues.get(position));
                }
            });
        }

        @Override
        public int getItemCount() {
            return mPresetIssues.size();
        }

        public class IssueViewHolder extends RecyclerView.ViewHolder {
            public TextView title;
            public TextView description;
            public ImageView imageView;
            public View container;

            public IssueViewHolder(View itemView) {
                super(itemView);
                title = (TextView) itemView.findViewById(R.id.title);
                description = (TextView) itemView.findViewById(R.id.description);
                imageView = (ImageView) itemView.findViewById(R.id.image_icon);
                container = itemView.findViewById(R.id.list_car_item);
            }
        }
    }

    /**
     * Onclick method for picking issue action
     *
     * @param view
     */
    public void pickAction(View view) {
    }

    /**
     * Onclick method for picking issue item
     *
     * @param view
     */
    public void pickItem(View view) {
    }

}

