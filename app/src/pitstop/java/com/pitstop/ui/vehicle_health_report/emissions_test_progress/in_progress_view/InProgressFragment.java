package com.pitstop.ui.vehicle_health_report.emissions_test_progress.in_progress_view;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
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
import com.github.florent37.viewanimator.AnimationListener;
import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.observer.BluetoothConnectionObservable;
import com.pitstop.ui.vehicle_health_report.emissions_test_progress.EmissionsProgressCallback;
import com.wang.avi.AVLoadingIndicatorView;

import java.util.Timer;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Matt on 2017-08-14.
 */

public class InProgressFragment extends Fragment implements InProgressView{
    private InProgressPresenter presenter;

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

    private Context context;
    private GlobalApplication application;

    public void setBluetooth(BluetoothConnectionObservable bluetooth){
        presenter.setBlueTooth(bluetooth);
    }

    public void setCallback(EmissionsProgressCallback callback){
        this.callback = callback;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        context = getActivity().getApplicationContext();
        application = (GlobalApplication)context;
        View view = inflater.inflate(R.layout.fragment_emissions_progress,container,false);
        ButterKnife.bind(this,view);
        UseCaseComponent component = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(application))
                .build();
        presenter = new InProgressPresenter(callback,component);
        progressAnimation.hide();


        bigButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                presenter.onBigButtonPressed();
            }
        });
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
              presenter.onBackPressed();
            }
        });
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
              presenter.onNextPressed();
            }
        });
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
    public void back() {
        cardSwitcher.setInAnimation(AnimationUtils.loadAnimation(context, R.anim.activity_slide_right_in));
        cardSwitcher.setOutAnimation(AnimationUtils.loadAnimation(context,R.anim.activity_slide_right_out));
        cardSwitcher.showPrevious();
    }

    @Override
    public void next() {
        cardSwitcher.setInAnimation(AnimationUtils.loadAnimation(context,R.anim.activity_slide_left_in));
        cardSwitcher.setOutAnimation(AnimationUtils.loadAnimation(context,R.anim.activity_slide_left_out));
        cardSwitcher.showNext();
    }

    @Override
    public int getCardNumber() {
        return cardSwitcher.getDisplayedChild();
    }

    @Override
    public void setReady() {
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
        com.github.florent37.viewanimator.ViewAnimator
                .animate(cardSwitcher.getCurrentView())
                .bounce()
                .duration(500L)
                .start();
    }

    @Override
    public void changeStep(String step) {
        com.github.florent37.viewanimator.ViewAnimator.animate(stepHolder)
                .fadeOut()
                .duration(500L)
                .onStop(new AnimationListener.Stop() {
                    @Override
                    public void onStop() {
                        stepText.setText(step);
                        com.github.florent37.viewanimator.ViewAnimator.animate(stepHolder)
                                .fadeIn()
                                .duration(500L)
                                .start();
                    }
                }).start();
    }

    @Override
    public void switchToProgress() {
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
                .onStop(new AnimationListener.Stop() {
                    @Override
                    public void onStop() {
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
                    }
                })
                .start();

    }

    @Override
    public void toast(String message) {
        Toast.makeText(context,message,Toast.LENGTH_LONG);//Long for testing purposes
    }

    @Override
    public void startTimer() {

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
}
