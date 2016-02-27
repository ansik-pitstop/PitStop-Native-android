package com.pitstop.parse;

import android.app.Application;

import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.parse.Parse;
import com.parse.ParseInstallation;
import com.pitstop.R;

import io.smooch.core.Smooch;

/**
 * Created by Ansik on 12/28/15.
 */
public class ParseApplication extends Application {

    public static MixpanelAPI mixpanelAPI;
    @Override
    public void onCreate() {
        super.onCreate();

//        ParseCrashReporting.enable(this);
        Parse.enableLocalDatastore(this);
        Parse.initialize(this, getString(R.string.parse_appID), getString(R.string.parse_clientID));
        ParseInstallation.getCurrentInstallation().saveInBackground();
        Smooch.init(this, "0xs5j98mds1x8mn77ptw4knc5");
        mixpanelAPI = MixpanelAPI.getInstance(this, "330c942ffad6819253501447810ad761");
    }
}
