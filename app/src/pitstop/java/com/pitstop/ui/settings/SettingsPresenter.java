package com.pitstop.ui.settings;

import android.content.Context;
import android.content.Intent;

import com.pitstop.application.LogOutable;
import com.pitstop.database.LocalCarAdapter;
import com.pitstop.database.UserAdapter;
import com.pitstop.interactors.GetCarsByUserIdUseCase;
import com.pitstop.interactors.GetCarsByUserIdUseCaseImpl;
import com.pitstop.models.Car;
import com.pitstop.utils.NetworkHelper;

import java.util.List;

import static com.pitstop.ui.main_activity.MainActivity.CAR_EXTRA;

/**
 * Created by Matt on 2017-06-12.
 */

public class SettingsPresenter {
    private SettingsInterface settings;
    private FragmentSwitcher switcher;
    private LogOutable logOutable; ;
    private GetCarsByUserIdUseCaseImpl getCarsByUserIdUseCase;
    public void subscribe(SettingsInterface settings, FragmentSwitcher switcher, LogOutable logOutable, Context context){
        getCarsByUserIdUseCase = new GetCarsByUserIdUseCaseImpl(new UserAdapter(context),new LocalCarAdapter(context),new NetworkHelper(context));
        this.settings = settings;
        this.switcher = switcher;
        this.logOutable = logOutable;
    }
    public void setViewMainSettings(){
        switcher.setViewMainSettings();
    }
    public void logout(){
        logOutable.logOutUser();
        settings.gotoLogin();
    }
    public void getCars(){
        getCarsByUserIdUseCase.execute(new GetCarsByUserIdUseCase.Callback(){
            @Override
            public void onCarsRetrieved(List<Car> cars) {
                System.out.println("Testing "+ cars);
            }
            @Override
            public void onError() {
                System.out.println("Testing error");
            }
        });
    }

    public void carAdded(Intent intent){
        Car car = intent.getParcelableExtra(CAR_EXTRA);

    }
    public void setViewCarSettings(){
        switcher.setViewCarSettings();
    }


}
