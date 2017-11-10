package com.pitstop.ui.settings;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ProgressBar;

import com.pitstop.R;
import com.pitstop.ui.settings.main_settings.MainSettingsFragment;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Matt on 2017-06-12.
 */

public class SettingsActivity extends AppCompatActivity implements SettingsView,FragmentSwitcher {
    private SettingsPresenter presenter;
    private FragmentManager fragmentManager;
    private MainSettingsFragment mainSettings;

    @BindView(R.id.settings_progress)
    ProgressBar loadingSpinner;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        ButterKnife.bind(this);
        fragmentManager = getFragmentManager();

        mainSettings = new MainSettingsFragment();
        mainSettings.setSwitcher(this);

        presenter = new SettingsPresenter(this);
        presenter.subscribe(this);
        presenter.setViewMainSettings();
    }

    @Override
    protected void onResume() {
        super.onResume();
        presenter.subscribe(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        presenter.unsubscribe();
    }

    @Override
    public void setViewMainSettings() {
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.settings_fragment_holder,mainSettings);
        fragmentTransaction.commit();
    }

    @Override
    public void loading(boolean show) {
        if(show){
            loadingSpinner.setVisibility(View.VISIBLE);
        }else{
            loadingSpinner.setVisibility(View.GONE);
        }
    }
}
