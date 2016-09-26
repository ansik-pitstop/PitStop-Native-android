package com.pitstop.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.pitstop.R;

/**
 * Created by yifan on 16/9/23.
 */
public class AddCustomIssueActivity extends AppCompatActivity {

    public static final String EXTRA_CAR_NAME = "car name";
    public static final String EXTRA_DEALERSHIP_NAME = "Dealership name";

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

    private void setupUI(){
        ((TextView)findViewById(R.id.car_name)).setText(getIntent().getStringExtra(EXTRA_CAR_NAME));
        ((TextView)findViewById(R.id.dealership_name)).setText(getIntent().getStringExtra(EXTRA_DEALERSHIP_NAME));
        mAction = (TextView) findViewById(R.id.custom_action);
        mItem = (TextView) findViewById(R.id.custom_item);
        mActionButton = (CardView) findViewById(R.id.custom_issue_action_button);
        mItemButton = (CardView) findViewById(R.id.custom_issue_item_button);

        mItemButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: 16/9/23 Do things
                Snackbar.make(rootView, "Item Button Tapped", Snackbar.LENGTH_LONG).show();
            }
        });

        mActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: 16/9/23 Do things
                Snackbar.make(rootView, "Action Button Tapped", Snackbar.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        // TODO: 16/9/23 Do things
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
        switch (item.getItemId()){
            case R.id.custom_issue_save:
                // TODO: 16/9/23 Do things
                Snackbar.make(rootView, "Save Issue Tapped", Snackbar.LENGTH_LONG).show();
                break;
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Onclick method for picking issue item
     * @param view
     */
    public void pickItem(View view) {

    }

    /**
     * Onclick method for picking issue action
     * @param view
     */
    public void pickAction(View view) {

    }
}
