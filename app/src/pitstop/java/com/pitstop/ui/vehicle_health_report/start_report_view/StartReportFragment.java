package com.pitstop.ui.vehicle_health_report.start_report_view;


import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.pitstop.R;
import com.pitstop.ui.vehicle_health_report.emissions_test_progress.EmissionsProgressActivity;
import com.pitstop.ui.vehicle_health_report.health_report_progress.ReportActivity;
import com.wang.avi.AVLoadingIndicatorView;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Matt on 2017-08-11.
 */

public class StartReportFragment extends Fragment implements StartReportView {

    @BindView(R.id.emissions_switch)
    Switch modeSwitch;

    @BindView(R.id.emissions_label)
    TextView emissionsLabel;

    @BindView(R.id.health_report_label)
    TextView healthReportLabel;

    @BindView(R.id.title_vehicle_health)
    TextView vehicleHealthTitle;

    @BindView(R.id.show_reports_button)
    Button pastReportsButton;

    @BindView(R.id.start_report_button)
    Button startReportButton;

    @BindView(R.id.start_report_animation)
    AVLoadingIndicatorView startAnimation;

    private boolean emissionsMode;


    private StartReportPresenter presenter;

    private Context context;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_start_report,container,false);
        context = getActivity().getApplicationContext();
        ButterKnife.bind(this,view);
        emissionsMode = false;
        presenter = new StartReportPresenter();
        startReportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(emissionsMode){
                    Intent intent = new Intent(getActivity(), EmissionsProgressActivity.class);
                    startActivity(intent);
                }else{
                    Intent intent = new Intent(getActivity(), ReportActivity.class);
                    startActivity(intent);
                }
            }
        });
        modeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                System.out.println("Testing "+b);
                presenter.onSwitchClicked(b);
            }
        });
        return view;
    }
    public static StartReportFragment newInstance() {
        StartReportFragment fragment = new StartReportFragment();
        return fragment;
    }

    @Override
    public void onResume() {
        super.onResume();
        presenter.subscribe(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        presenter.unsubscribe();
    }

    @Override
    public void setModeEmissions() {
        emissionsMode = true;
        emissionsLabel.setTextColor(ContextCompat.getColor(context,R.color.highlight));
        healthReportLabel.setTextColor(ContextCompat.getColor(context,R.color.dark_grey));
        vehicleHealthTitle.setTextColor(ContextCompat.getColor(context,R.color.highlight));
        pastReportsButton.setBackground(ContextCompat.getDrawable(context,R.drawable.color_button_green_highlight));
        startReportButton.setBackground(ContextCompat.getDrawable(context,R.drawable.color_button_green_highlight));
        startAnimation.setIndicatorColor(ContextCompat.getColor(context,R.color.highlight));
        modeSwitch.getThumbDrawable().setColorFilter(ContextCompat.getColor(context,R.color.highlight), PorterDuff.Mode.MULTIPLY);
    }


    @Override
    public void setModeHealthReport() {
        emissionsMode = false;
        emissionsLabel.setTextColor(ContextCompat.getColor(context,R.color.dark_grey));
        healthReportLabel.setTextColor(ContextCompat.getColor(context,R.color.primary));
        vehicleHealthTitle.setTextColor(ContextCompat.getColor(context,R.color.primary));
        pastReportsButton.setBackground(ContextCompat.getDrawable(context,R.drawable.color_button_primary));
        startReportButton.setBackground(ContextCompat.getDrawable(context,R.drawable.color_button_primary));
        startAnimation.setIndicatorColor(ContextCompat.getColor(context,R.color.primary));
        modeSwitch.getThumbDrawable().setColorFilter(ContextCompat.getColor(context,R.color.primary), PorterDuff.Mode.MULTIPLY);
    }
}
