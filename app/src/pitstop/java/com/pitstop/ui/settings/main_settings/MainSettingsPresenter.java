package com.pitstop.ui.settings.main_settings;

import com.pitstop.R;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.get.GetCurrentUserUseCase;
import com.pitstop.interactors.update.UpdateUserNameUseCase;
import com.pitstop.interactors.update.UpdateUserPhoneUseCase;
import com.pitstop.models.User;
import com.pitstop.network.RequestError;
import com.pitstop.ui.settings.FragmentSwitcher;
import com.pitstop.utils.MixpanelHelper;


/**
 * Created by Matt on 2017-06-12.
 */

public class MainSettingsPresenter {
    private final String NAME_PREF_KEY = "pref_username_key";
    private final String PHONE_PREF_KEY = "pref_phone_number_key";
    private final String ADD_CAR_KEY = "add_car_button";
    private final String PRIV_KEY = "pref_privacy_policy";
    private  final String TERMS_KEY = "pref_term_of_use";
    private final String LOG_OUT_KEY = "pref_logout";

    private MainSettingsView mainSettings;
    private FragmentSwitcher switcher;

    private UseCaseComponent component;


    private MixpanelHelper mixpanelHelper;


    public MainSettingsPresenter(FragmentSwitcher switcher, UseCaseComponent component, MixpanelHelper mixpanelHelper){
        this.switcher = switcher;

        this.component = component;
        this.mixpanelHelper = mixpanelHelper;
    }

    public void subscribe(MainSettingsView mainSettings ){
        if(switcher == null){return;}

        mixpanelHelper.trackViewAppeared("MainSettings");
        this.mainSettings = mainSettings;
        if(switcher == null){return;}
        switcher.loading(true);
    }

    public void unsubscribe(){
        this.mainSettings = null;
    }


    public void setVersion(){
        if(mainSettings == null){return;}
        mainSettings.showVersion(mainSettings.getBuildNumber());
    }

    public void preferenceClicked(String prefKey){
        if(mainSettings == null || switcher ==null){return;}
        if(prefKey.equals(PRIV_KEY)){
            mixpanelHelper.trackButtonTapped("PrivacyPolicy","MainSettings");
            mainSettings.startPriv();
        }else if(prefKey.equals(TERMS_KEY)){
            mixpanelHelper.trackButtonTapped("TermsOfService","MainSettings");
            mainSettings.startTerms();
        }else if(prefKey.equals(LOG_OUT_KEY)){
            mixpanelHelper.trackButtonTapped("LogOut","MainSettings");
            mainSettings.showLogOut();
        }
    }

    public void update(){
        if(mainSettings == null || switcher == null){return;}
        switcher.loading(true);
        getUser();

    }

    public void getUser(){
        if(mainSettings == null){return;}
        component.getGetCurrentUserUseCase().execute(new GetCurrentUserUseCase.Callback() {
            @Override
            public void onUserRetrieved(User user) {
                if(mainSettings != null){
                    String username = user.getFirstName() + " " + user.getLastName();
                    String phone = user.getPhone();
                    mainSettings.showName(username);
                    mainSettings.showPhone(phone);
                    mainSettings.showEmail(user.getEmail());
                    mainSettings.setPrefs(username,phone);
                    switcher.loading(false);
                }
            }

            @Override
            public void onError(RequestError error) {
                if(mainSettings != null){
                    switcher.loading(false);
                    mainSettings.toast(error.getMessage());
                }
            }
        });
    }


    public void preferenceInput(String text, String key){
        if(mainSettings == null){return;}
        if(key.equals(NAME_PREF_KEY)){
            mixpanelHelper.trackButtonTapped("Name","MainSettings");
            mainSettings.showName(text);
            component.getUpdateUserNameUseCase().execute(text, new UpdateUserNameUseCase.Callback() {
                @Override
                public void onUserNameUpdated() {
                }

                @Override
                public void onError(RequestError error) {
                    if (mainSettings != null)
                        mainSettings.toast(error.getMessage());
                }
            });
        }else if(key.equals(PHONE_PREF_KEY)){
            mixpanelHelper.trackButtonTapped("Phone","MainSettings");
            String prevNum = mainSettings.getDisplayedPhoneNum();
            if (text.length() < 9 || text.length() > 15){
                mainSettings.toast(R.string.invalid_phone_error);
                return;

            }
            mainSettings.showPhone(text);
            component.getUpdateUserPhoneUseCase().execute(text, new UpdateUserPhoneUseCase.Callback() {
                @Override
                public void onUserPhoneUpdated() {

                }

                @Override
                public void onError(RequestError error) {
                    if (mainSettings != null){
                        if (prevNum != null){
                            mainSettings.showPhone(prevNum);
                            mainSettings.toast(error.getMessage());
                        }
                    }
                }
            });

        }
    }
    public void logout(){
        if(mainSettings == null){return;}
        mainSettings.logout();
        mainSettings.gotoLogin();
    }
}
