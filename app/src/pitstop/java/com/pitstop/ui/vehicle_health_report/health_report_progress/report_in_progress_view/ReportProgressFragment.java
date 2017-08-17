package com.pitstop.ui.vehicle_health_report.health_report_progress.report_in_progress_view;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.florent37.viewanimator.AnimationListener;
import com.github.florent37.viewanimator.ViewAnimator;
import com.pitstop.R;
import com.pitstop.ui.vehicle_health_report.health_report_progress.ReportPresenter;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Matt on 2017-08-16.
 */

public class ReportProgressFragment extends Fragment implements ReportProgressView {

    private ReportProgressPresenter presenter;

    @BindView(R.id.step_holder)
    LinearLayout stepHolder;

    @BindView(R.id.step_text)
    TextView stepText;

    @BindView(R.id.start_report_button)
    Button bigButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragement_report_progress,container,false);
        ButterKnife.bind(this,view);
        presenter = new ReportProgressPresenter();

        bigButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                presenter.changeStep("Step 2");
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        presenter.subscirbe(this);
    }

    @Override
    public void changeStep(String step) {
        ViewAnimator.animate(stepHolder)
                .fadeOut()
                .duration(500L)
                .onStop(new AnimationListener.Stop() {
                    @Override
                    public void onStop() {
                        stepText.setText(step);
                        ViewAnimator.animate(stepHolder)
                                .fadeIn()
                                .duration(500L)
                                .start();
                    }
                }).start();
    }
}
