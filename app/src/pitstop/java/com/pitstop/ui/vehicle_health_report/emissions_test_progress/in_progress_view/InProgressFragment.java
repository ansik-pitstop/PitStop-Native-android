package com.pitstop.ui.vehicle_health_report.emissions_test_progress.in_progress_view;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewAnimator;

import com.daimajia.numberprogressbar.NumberProgressBar;
import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.observer.BluetoothConnectionObservable;
import com.pitstop.ui.vehicle_health_report.emissions_test_progress.EmissionsProgressCallback;
import com.wang.avi.AVLoadingIndicatorView;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Matt on 2017-08-14.
 */

public class InProgressFragment extends Fragment implements InProgressView{

    private final String TAG = getClass().getSimpleName();
    private final String WAITING_ANIMATION = "BallScaleIndicator";
    private final String INPROGRESS_ANIMATION = "BallClipRotatePulseIndicator";

    @BindView(R.id.progress_report_animation)
    AVLoadingIndicatorView progressAnimation;

    @BindView(R.id.emissions_progress_bar)
    NumberProgressBar emissionsProgressBar;

    @BindView(R.id.emissions_card_switcher)
    ViewAnimator cardSwitcher;

    @BindView(R.id.start_report_button)
    Button bigButton;

    @BindView(R.id.start_report_animation)
    AVLoadingIndicatorView loadingAnimation;

    @BindView(R.id.emissions_next_button)
    Button nextButton;

    @BindView(R.id.emissions_back_button)
    Button backButton;

    @BindView(R.id.emissions_progress_lower)
    RelativeLayout emissionsLower;

    @BindView(R.id.start_report_button_holder)
    FrameLayout bigButtonHolder;

    @BindView(R.id.emissions_root)
    RelativeLayout emissiosRoot;

    @BindView(R.id.emissions_start_text)
    TextView startText;
    @BindView(R.id.emissions_pitstop_logo)
    ImageView pitstopLogo;

    @BindView(R.id.step_holder)
    LinearLayout stepHolder;

    @BindView(R.id.step_text)
    TextView stepText;

    private EmissionsProgressCallback callback;
    private InProgressPresenter presenter;
    private Context context;
    private GlobalApplication application;

    public void setBluetooth(BluetoothConnectionObservable bluetooth){
        Log.d(TAG,"setBluetooth()");
        presenter.setBluetooth(bluetooth);
    }

    public void setCallback(EmissionsProgressCallback callback){
        Log.d(TAG,"setCallback()");
        this.callback = callback;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG,"onCreateView()");
        context = getActivity().getApplicationContext();
        application = (GlobalApplication)context;
        View view = inflater.inflate(R.layout.fragment_emissions_progress,container,false);
        ButterKnife.bind(this,view);
        UseCaseComponent component = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(application))
                .build();
        presenter = new InProgressPresenter(callback,component);
        progressAnimation.hide();


        bigButton.setOnClickListener(view1 -> presenter.onBigButtonPressed());
        backButton.setOnClickListener(view12 -> presenter.onBackPressed());
        nextButton.setOnClickListener(view13 -> presenter.onNextPressed());
        return view;
    }

    @Override
    public void onResume() {
        Log.d(TAG,"onResume()");
        super.onResume();
        presenter.subscribe(this);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG,"onDestroy()");
        super.onDestroy();
        presenter.unsubscribe();
    }

    @Override
    public void back() {
        Log.d(TAG,"back()");
        cardSwitcher.setInAnimation(AnimationUtils.loadAnimation(context, R.anim.activity_slide_right_in));
        cardSwitcher.setOutAnimation(AnimationUtils.loadAnimation(context,R.anim.activity_slide_right_out));
        cardSwitcher.showPrevious();
    }

    @Override
    public void next() {
        Log.d(TAG,"next()");
        cardSwitcher.setInAnimation(AnimationUtils.loadAnimation(context,R.anim.activity_slide_left_in));
        cardSwitcher.setOutAnimation(AnimationUtils.loadAnimation(context,R.anim.activity_slide_left_out));
        cardSwitcher.showNext();
    }

    @Override
    public int getCardNumber() {
        Log.d(TAG,"getCardNumber()");
        return cardSwitcher.getDisplayedChild();
    }

    @Override
    public void setReady() {
        Log.d(TAG,"setReady()");
        loadingAnimation.setVisibility(View.VISIBLE);
        loadingAnimation.smoothToShow();
        startText.setVisibility(View.VISIBLE);
        com.github.florent37.viewanimator.ViewAnimator
                .animate(pitstopLogo)
                .fadeOut()
                .duration(500L)
                .andAnimate(startText)
                .fadeIn()
                .duration(500L)
                .start();

    }

    @Override
    public void bounceCards() {
        Log.d(TAG,"bounceCards()");
        com.github.florent37.viewanimator.ViewAnimator
                .animate(cardSwitcher.getCurrentView())
                .bounce()
                .duration(500L)
                .start();
    }

    @Override
    public void changeStep(String step) {
        Log.d(TAG,"changeStep() step: "+step);
        com.github.florent37.viewanimator.ViewAnimator.animate(stepHolder)
                .fadeOut()
                .duration(500L)
                .onStop(() -> {
                    stepText.setText(step);
                    com.github.florent37.viewanimator.ViewAnimator.animate(stepHolder)
                            .fadeIn()
                            .duration(500L)
                            .start();
                }).start();
    }

    @Override
    public void switchToProgress() {
        Log.d(TAG,"switchToProgress()");
        bigButton.setEnabled(false);

        loadingAnimation.hide();
        progressAnimation.show();

        com.github.florent37.viewanimator.ViewAnimator
                .animate(emissionsLower)
                .duration(500L)
                .fadeOut()
                .andAnimate(startText)
                .fadeOut()
                .duration(500L)
                .andAnimate(pitstopLogo)
                .fadeIn()
                .duration(500L)
                .start();

        com.github.florent37.viewanimator.ViewAnimator
                .animate(bigButtonHolder)
                .dp().translationY(0,50)
                .duration(500L)
                .onStop(() -> {
                    emissionsProgressBar.setVisibility(View.VISIBLE);
                    stepHolder.setVisibility(View.VISIBLE);
                    com.github.florent37.viewanimator.ViewAnimator
                            .animate(emissionsProgressBar)
                            .fadeIn()
                            .duration(500L)
                            .andAnimate(stepHolder)
                            .fadeIn()
                            .duration(500L)
                            .start();
                })
                .start();

    }

    @Override
    public void toast(String message) {
        Log.d(TAG,"toast() message: "+message);
        Toast.makeText(context,message,Toast.LENGTH_LONG);//Long for testing purposes
    }

    @Override
    public void startTimer() {
        Log.d(TAG,"startTimer()");
        new CountDownTimer(10000,100){
            @Override
            public void onTick(long l) {
                int tick = 10000-((int)l);
                emissionsProgressBar.setProgress(tick/100);
            }

            @Override
            public void onFinish() {
                presenter.showReport();
            }
        }.start();
    }

    @Override
    public void endProgress(String message) {
        bigButton.setEnabled(true);

        com.github.florent37.viewanimator.ViewAnimator.animate(stepHolder)
                .fadeOut()
                .duration(500L)
                .onStop(() -> {
                    stepText.setText(message);
                    com.github.florent37.viewanimator.ViewAnimator.animate(stepHolder)
                            .fadeIn()
                            .duration(500L)
                            .wave()
                            .duration()
                            .onStop(() -> {
                                com.github.florent37.viewanimator.ViewAnimator
                                    .animate(emissionsLower)
                                    .duration(500L)
                                    .fadeIn()
                                    .andAnimate(startText)
                                    .fadeIn()
                                    .duration(500L)
                                    .andAnimate(pitstopLogo)
                                    .fadeOut()
                                    .duration(500L)
                                    .start();

                                com.github.florent37.viewanimator.ViewAnimator
                                    .animate(bigButtonHolder)
                                    .dp().translationY(50,0)
                                    .duration(500L)
                                    .onStop(() -> {
                                        emissionsProgressBar.setVisibility(View.GONE);
                                        stepHolder.setVisibility(View.GONE);
                                        com.github.florent37.viewanimator.ViewAnimator
                                                .animate(emissionsProgressBar)
                                                .fadeOut()
                                                .duration(500L)
                                                .andAnimate(stepHolder)
                                                .fadeOut()
                                                .duration(500L)
                                                .start();
                                    })
                                    .start();
                                progressAnimation.hide();
                                loadingAnimation.show();
                            })
                            .start();
                }).start();

    }

}
