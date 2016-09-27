package com.pitstop.ui;

import android.app.AlertDialog;
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
import android.widget.Toast;

import com.pitstop.R;

import java.util.HashMap;

import static com.pitstop.R.drawable.severity_critical_indicator;
import static com.pitstop.R.drawable.severity_high_indicator;
import static com.pitstop.R.drawable.severity_low_indicator;
import static com.pitstop.R.drawable.severity_medium_indicator;

/**
 * Created by yifan on 16/9/23.
 */
public class AddCustomIssueActivity extends AppCompatActivity {

    private static final String TAG = AddCustomIssueActivity.class.getSimpleName();

    private static final String SERVICE_EMERGENCY = "Emergency";
    private static final String SERVICE_REPLACE = "Replace";
    private static final String ITEM_FLAT_TIRE = "Flat Tire";
    private static final String ITEM_TOW_TRUCK = "Tow Truck";
    private static final String ITEM_ENGINE_OIL_FILTER = "Engine Oil & Filter";
    private static final String ITEM_WIPERS_FLUIDS = "Wipers/Fluids";

    public static final String EXTRA_CAR_NAME = "car name";
    public static final String EXTRA_DEALERSHIP_NAME = "Dealership name";

    private String mPickedAction;
    private String mPickedItem;
    private String mDescription = "";
    private int mSeverity = 0;

    private View rootView;
    private TextView mAction;
    private TextView mItem;
    private CardView mActionButton;
    private CardView mItemButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rootView = getLayoutInflater().inflate(R.layout.activity_add_custom_issue, null);
        setContentView(rootView);
        overridePendingTransition(R.anim.activity_bottom_up_in, R.anim.activity_bottom_up_out);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setupUI();
    }

    private void setupUI() {
        ((TextView) findViewById(R.id.car_name)).setText(getIntent().getStringExtra(EXTRA_CAR_NAME));
        ((TextView) findViewById(R.id.dealership_name)).setText(getIntent().getStringExtra(EXTRA_DEALERSHIP_NAME));
        mAction = (TextView) findViewById(R.id.custom_action);
        mItem = (TextView) findViewById(R.id.custom_item);
        mActionButton = (CardView) findViewById(R.id.custom_issue_action_button);
        mItemButton = (CardView) findViewById(R.id.custom_issue_item_button);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        // TODO: 16/9/23 Do things
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
                // TODO: 16/9/27 Save and post issue
                Snackbar.make(rootView, "Save Issue Tapped", Snackbar.LENGTH_LONG).show();
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
        AlertDialog.Builder dialog = new AlertDialog.Builder(AddCustomIssueActivity.this)
                .setTitle("Please pick the service category")
                .setItems(getResources().getStringArray(R.array.preset_issue_actions),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // TODO: 16/9/27 clear previous picked item
                                // TODO: 16/9/27 reset visibility of items

                                mPickedAction = getResources().getStringArray(R.array.preset_issue_actions)[which];
                                mAction.setText(mPickedAction);
                                Snackbar.make(rootView, "Action Button Tapped", Snackbar.LENGTH_LONG).show();
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
        AlertDialog.Builder dialog = new AlertDialog.Builder(AddCustomIssueActivity.this)
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
                            mSeverity = 5;
                        } else if (mPickedItem.equals(ITEM_TOW_TRUCK)) {
                            mDescription = getString(R.string.tow_truck_description);
                            mSeverity = 5;
                        } else if (mPickedItem.equals(ITEM_ENGINE_OIL_FILTER)){
                            mDescription = getString(R.string.engine_oil_filter_description);
                            mSeverity = 3;
                        } else if (mPickedItem.equals(ITEM_WIPERS_FLUIDS)){
                            mDescription = getString(R.string.wipers_fluids_description);
                            mSeverity = 2;
                        }

                        Snackbar.make(rootView, "Item Button Tapped", Snackbar.LENGTH_LONG).show();

                        setupDescriptionView();
                    }
                })
                .setCancelable(false);
        dialog.show();
    }

    private void showSaveDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(AddCustomIssueActivity.this)
                .setTitle("Save custom issue")
                .setMessage("You have not saved your custom issue yet! Are you sure you want to quit?")
                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        // TODO: 16/9/27 Save items

                        AddCustomIssueActivity.super.onBackPressed();
                        overridePendingTransition(R.anim.activity_bottom_down_in, R.anim.activity_bottom_down_out);
                        finish();
                    }
                })
                .setNegativeButton("CANCEL", null);
        dialog.show();
    }

    private void setupDescriptionView() {
        RelativeLayout severityIndicatorLayout = (RelativeLayout) findViewById(R.id.custom_severity_indicator_layout);
        TextView severityTextView = (TextView) findViewById(R.id.custom_issue_severity_text);

        String title = mPickedAction + " " + mPickedItem;
        ((TextView) findViewById(R.id.custom_issue_title_text)).setText(title);
        ((TextView) findViewById(R.id.custom_issue_description)).setText(mDescription);

        // TODO: 16/9/27 Set indicator color

        switch (mSeverity) {
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
                severityIndicatorLayout.setBackground(ContextCompat.getDrawable(this, severity_critical_indicator));
                severityTextView.setText(getResources().getStringArray(R.array.severity_indicators)[3]);
                break;
        }

    }

}
