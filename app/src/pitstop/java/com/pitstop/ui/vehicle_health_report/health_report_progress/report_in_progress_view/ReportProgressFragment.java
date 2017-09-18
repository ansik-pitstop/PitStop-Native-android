package com.pitstop.ui.vehicle_health_report.health_report_progress.report_in_progress_view;

import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.daimajia.numberprogressbar.NumberProgressBar;
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
import butterknife.OnClick;

/**
 * Created by Matt on 2017-08-16.
 */

public class ReportProgressFragment extends Fragment implements ReportProgressView {

    private final String TAG = getClass().getSimpleName();

    @BindView(R.id.progress_bar)
    NumberProgressBar progressBar;

    @BindView(R.id.step_holder)
    LinearLayout stepHolder;

    @BindView(R.id.step_text)
    TextView stepText;

    @BindView(R.id.start_report_button)
    Button bigButton;

    @BindView(R.id.error_holder)
    View errorHolder;

    @BindView(R.id.error_text)
    TextView errorText;

    private ReportCallback callback;
    private Context context;
    private GlobalApplication application;
    private BluetoothConnectionObservable bluetooth;
    private ReportProgressPresenter presenter;

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
        progressBar.setMax(100);
        UseCaseComponent component = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(application))
                .build();
        presenter = new ReportProgressPresenter(callback,component);

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        Log.d(TAG,"onViewCreated()");
        super.onViewCreated(view, savedInstanceState);
        presenter.subscribe(this);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG,"onDestroy()");
        super.onDestroy();
        presenter.unsubscribe();
    }

    @Override
    public void changeStep(String step) {
        Log.d(TAG,"changeStep() step: "+step);
        ViewAnimator.animate(stepHolder)
                .fadeOut()
                .duration(500L)
                .onStop(() -> {
                    stepText.setText(step);
                    ViewAnimator.animate(stepHolder)
                            .fadeIn()
                            .duration(500L)
                            .start();
                }).start();
    }

    @OnClick(R.id.error_button)
    public void onErrorButtonClicked(){
        Log.d(TAG,"onErrorButtonClicked()");
        presenter.onErrorButtonClicked();
    }

    @Override
    public void setLoading(int progress) {
        Log.d(TAG,"setLoading() progress: "+progress);
        progressBar.setProgress(progress);
    }

    @Override
    public void showError(String title, String body, DialogInterface.OnClickListener onOkClicked) {
        Log.d(TAG,"showError() title: "+title+", body: "+body);
        ViewAnimator.animate(stepHolder)
                .fadeOut()
                .duration(500L)
                .onStop(() -> {
                    stepHolder.setVisibility(View.INVISIBLE);
                    errorHolder.setVisibility(View.VISIBLE);
                    errorText.setText(body);
                    ViewAnimator.animate(errorHolder)
                            .fadeIn()
                            .duration(500L)
                            .start();

                })
                .start();
    }
}
