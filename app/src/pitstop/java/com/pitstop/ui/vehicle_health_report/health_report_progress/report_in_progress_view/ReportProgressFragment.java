package com.pitstop.ui.vehicle_health_report.health_report_progress.report_in_progress_view;

import android.app.Fragment;
import android.content.Context;
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
import com.pitstop.application.GlobalApplication;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.observer.BluetoothConnectionObservable;
import com.pitstop.ui.vehicle_health_report.health_report_progress.ReportCallback;

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

    private ReportCallback callback;

    private Context context;
    private GlobalApplication application;

    private BluetoothConnectionObservable bluetooth;


    public void setCallback(ReportCallback callback){
        this.callback = callback;
    }

    public void setBluetooth(BluetoothConnectionObservable bluetooth){
        this.bluetooth = bluetooth;
        presenter.setBluetooth(bluetooth);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        context = getActivity().getApplicationContext();
        application = (GlobalApplication) context;
        View view = inflater.inflate(R.layout.fragement_report_progress,container,false);
        ButterKnife.bind(this,view);
        UseCaseComponent component = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(application))
                .build();
        presenter = new ReportProgressPresenter(callback,component);
        return view;
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
