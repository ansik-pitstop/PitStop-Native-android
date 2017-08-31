package com.pitstop.ui.services.custom_service;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.pitstop.R;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.ui.services.custom_service.view_fragments.ServiceFormFragment;

/**
 * Created by Matt on 2017-07-25.
 */

public class CustomServiceActivity extends AppCompatActivity implements CustomServiceView,CustomServiceActivityCallback {
    private CustomServicePresenter presenter;
    private FragmentManager fragmentManager;
    private ServiceFormFragment serviceFormFragment;

    private boolean historical;


    public static String HISTORICAL_EXTRA = "historical";


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_service);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        historical = getIntent().getExtras().getBoolean(HISTORICAL_EXTRA);
        fragmentManager = getFragmentManager();
        serviceFormFragment = new ServiceFormFragment();
        presenter = new CustomServicePresenter();
    }

    @Override
    protected void onResume() {
        super.onResume();
        presenter.subscribe(this);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == android.R.id.home){
            super.onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void setViewServiceForm() {
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.custom_service_view_holder, serviceFormFragment);
        fragmentTransaction.commit();
    }

    @Override
    public boolean getHistorical() {
        return historical;
    }

    @Override
    public void finishForm(CarIssue issue) {
        Intent intent = new Intent();
        intent.putExtra(CarIssue.class.getName(),issue);
        finish();
    }
}
